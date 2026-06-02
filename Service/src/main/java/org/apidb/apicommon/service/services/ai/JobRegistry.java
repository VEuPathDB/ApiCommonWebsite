package org.apidb.apicommon.service.services.ai;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

  private static final JobRegistry INSTANCE = new JobRegistry();

  public static JobRegistry instance() { return INSTANCE; }

  private final ConcurrentHashMap<String, JobState> _jobs = new ConcurrentHashMap<>();
  private final ExecutorService _executor = Executors.newFixedThreadPool(POOL_SIZE);
  private final ScheduledExecutorService _evictor = Executors.newSingleThreadScheduledExecutor();

  private JobRegistry() {
    // Eviction sweep is scheduled in deliverable 8.
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
    throw new UnsupportedOperationException("JobRegistry.submit — deliverable 1");
  }

  /** Attach a caller as a follower of an already-in-flight job. */
  public JobState attach(String jobId, long userId) {
    throw new UnsupportedOperationException("JobRegistry.attach — deliverable 1");
  }

  /** Cancel an in-flight job: cancel the future and any in-flight LLM HTTP call. */
  public void cancel(String jobId) {
    throw new UnsupportedOperationException("JobRegistry.cancel — deliverable 7");
  }
}
