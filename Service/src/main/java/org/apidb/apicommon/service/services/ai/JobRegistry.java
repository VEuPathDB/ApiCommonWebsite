package org.apidb.apicommon.service.services.ai;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apidb.apicommon.model.comment.pojo.JobStatus;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.log4j.Logger;

/**
 * In-memory store of in-flight and recently-terminal jobs, keyed by the hex
 * SHA-256 {@code jobId}. Backs cache hits for double-tap submits and
 * cross-user resubmissions; the durable {@code comment_ai_run} rows satisfy
 * hits after a registry entry is evicted.
 *
 * <p>Process-wide singleton: one bounded executor and one eviction scheduler
 * per JVM. (Lifecycle is intentionally simple for v1 — flagged at review; a
 * future revision could bind this to the application/WdkModel lifecycle.)
 */
public class JobRegistry {

  /** Bounded pool sized for slow LLM calls (plan: 8 threads). */
  public static final int POOL_SIZE = 8;

  /** Terminal jobs are evicted once their terminal-state age exceeds this. */
  public static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

  /** How often the eviction sweep runs. */
  public static final long EVICTION_PERIOD_SECONDS = 60;

  private static final Logger LOG = Logger.getLogger(JobRegistry.class);

  private static final JobRegistry INSTANCE = new JobRegistry();

  public static JobRegistry instance() { return INSTANCE; }

  private final ConcurrentHashMap<String, JobState> _jobs = new ConcurrentHashMap<>();
  private final ExecutorService _executor;
  private final ScheduledExecutorService _evictor;

  private JobRegistry() {
    this(Executors.newFixedThreadPool(POOL_SIZE), Executors.newSingleThreadScheduledExecutor());
  }

  /** Visible for testing: inject the bounded executor and eviction scheduler. */
  JobRegistry(ExecutorService executor, ScheduledExecutorService evictor) {
    _executor = executor;
    _evictor = evictor;
    // Periodically reap terminal jobs whose TTL has elapsed. The durable
    // comment_ai_run rows still satisfy late cache hits, so eviction only frees
    // memory — it never loses a publishable result.
    _evictor.scheduleAtFixedRate(this::sweepQuietly,
        EVICTION_PERIOD_SECONDS, EVICTION_PERIOD_SECONDS, TimeUnit.SECONDS);
  }

  /** Look up an existing job by its digest. */
  public Optional<JobState> get(String jobId) {
    return Optional.ofNullable(_jobs.get(jobId));
  }

  /**
   * Register a fresh job for the given submission and submit its pipeline to the
   * bounded executor. The {@code pipelineFactory} builds the {@link Runnable}
   * from the freshly-created {@link JobState} (the pipeline needs the state the
   * registry owns), resolving the construction order.
   *
   * @throws RejectedExecutionException if the pool is saturated; the caller
   *         translates this to {@code 503 Service Unavailable} + {@code Retry-After}.
   */
  public JobState submit(JobSubmission submission, long userId,
      Function<JobState, Runnable> pipelineFactory) {
    String jobId = submission.getJobId();
    JobState created = new JobState(submission, userId);

    // Atomically claim the digest. If another caller already started this exact
    // job (race between the prelude's miss-check and here), attach instead.
    JobState existing = _jobs.putIfAbsent(jobId, created);
    if (existing != null) {
      existing.addFollower(userId);
      return existing;
    }

    try {
      Future<?> future = _executor.submit(pipelineFactory.apply(created));
      created.setFuture(future);
    }
    catch (RejectedExecutionException e) {
      // Pool saturated: back the job out so a later retry can re-submit cleanly.
      // The caller translates this to 503 + Retry-After.
      _jobs.remove(jobId, created);
      throw e;
    }
    return created;
  }

  /** Attach a caller as a follower of an already-in-flight job (null if absent). */
  public JobState attach(String jobId, long userId) {
    JobState job = _jobs.get(jobId);
    if (job != null) {
      job.addFollower(userId);
    }
    return job;
  }

  /**
   * Cancel a job for all attached followers (v1 has no per-follower detach).
   * No-op if the job is unknown / already evicted, or already terminal. Marks
   * the job {@code cancelled} <em>before</em> interrupting the pipeline thread so
   * the cancellation wins the race against the interrupted stage's
   * {@code internal-error} unwinding (see {@link JobState#markTerminal}). The
   * cancelled job stays in the registry so the next poll observes it (TTL
   * eviction reaps it later).
   */
  public void cancel(String jobId) {
    JobState job = _jobs.get(jobId);
    if (job == null) return;  // unknown or already evicted — nothing to cancel

    job.markTerminal(JobStatus.CANCELLED, null);  // first-wins; no-op if already terminal

    Future<?> future = job.getFuture();
    if (future != null)
      future.cancel(true);  // interrupt any in-flight blocking work (LLM/HTTP call)
  }

  /**
   * Remove every terminal job whose terminal age has exceeded {@link #TTL_MILLIS}
   * at {@code now}. Running jobs are never evicted. Pure given {@code now}
   * (visible for testing); the scheduled sweep passes the current wall clock.
   *
   * @return the number of entries evicted
   */
  int sweep(long now) {
    AtomicInteger evicted = new AtomicInteger();
    // ConcurrentHashMap's removeIf is weakly-consistent and per-entry atomic, so
    // a concurrent submit() of a fresh job is never clobbered by the sweep.
    _jobs.values().removeIf(job -> {
      if (job.isExpiredAt(now, TTL_MILLIS)) {
        evicted.incrementAndGet();
        return true;
      }
      return false;
    });
    return evicted.get();
  }

  /** Scheduler entry point: sweep at the current time, swallowing any error so the recurring task survives. */
  private void sweepQuietly() {
    try {
      int evicted = sweep(System.currentTimeMillis());
      if (evicted > 0 && LOG.isDebugEnabled())
        LOG.debug("Evicted " + evicted + " terminal AI job(s) past TTL; " + _jobs.size() + " remain");
    }
    catch (RuntimeException e) {
      // Never let a sweep failure kill the recurring schedule (scheduleAtFixedRate
      // suppresses all future runs if the task throws).
      LOG.error("AI job-registry eviction sweep failed; will retry next period", e);
    }
  }
}
