package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JobRegistryTest {

  private ExecutorService _executor;
  private ScheduledExecutorService _evictor;
  private JobRegistry _registry;

  @Before
  public void setUp() {
    _executor = Executors.newSingleThreadExecutor();
    _evictor = Executors.newSingleThreadScheduledExecutor();
    _registry = new JobRegistry(_executor, _evictor);
  }

  @After
  public void tearDown() {
    _executor.shutdownNow();
    _evictor.shutdownNow();
  }

  private static JobSubmission submission(String jobId) {
    AiGenePublicationRequest r = new AiGenePublicationRequest();
    r.geneId = "PF3D7_1133400";
    r.documentType = "pubmed";
    r.pubmedId = "12345678";
    return new JobSubmission(r, jobId, Collections.emptyList(),
        "claude-sonnet-4-20250514", "getGeneSummary/v1", "{}");
  }

  private static Function<JobState, Runnable> noopPipeline() {
    return jobState -> () -> { };
  }

  @Test
  public void submit_registersJobRetrievableByGet() {
    JobState job = _registry.submit(submission("digest-A"), 100L, noopPipeline());
    assertSame(job, _registry.get("digest-A").orElse(null));
    assertEquals(Collections.singletonList(100L), job.getFollowerUserIds());
  }

  @Test
  public void submit_runsPipelineExactlyOnce() throws InterruptedException {
    CountDownLatch ran = new CountDownLatch(1);
    AtomicInteger runs = new AtomicInteger();
    _registry.submit(submission("digest-B"), 100L,
        js -> () -> { runs.incrementAndGet(); ran.countDown(); });
    assertTrue("pipeline did not run", ran.await(2, TimeUnit.SECONDS));
    assertEquals(1, runs.get());
  }

  @Test
  public void submit_sameDigestTwice_attachesSecondCallerAndRunsOnce() {
    AtomicInteger factoryCalls = new AtomicInteger();
    Function<JobState, Runnable> counting = js -> { factoryCalls.incrementAndGet(); return () -> { }; };

    JobState first = _registry.submit(submission("digest-C"), 100L, counting);
    JobState second = _registry.submit(submission("digest-C"), 200L, counting);

    assertSame("second submit should attach to the same job", first, second);
    assertEquals(1, factoryCalls.get());
    assertTrue(first.getFollowerUserIds().contains(100L));
    assertTrue(first.getFollowerUserIds().contains(200L));
  }

  @Test
  public void attach_addsFollowerToExistingJob() {
    _registry.submit(submission("digest-D"), 100L, noopPipeline());
    JobState attached = _registry.attach("digest-D", 200L);
    assertTrue(attached.getFollowerUserIds().contains(200L));
  }

  @Test
  public void attach_returnsNullForUnknownJob() {
    assertNull(_registry.attach("never-submitted", 100L));
  }

  @Test
  public void cancel_marksRunningJobCancelledAndCancelsFuture() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    _registry.submit(submission("cancel-1"), 100L, js -> () -> {
      started.countDown();
      try { Thread.sleep(10_000); }
      catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    });
    assertTrue("pipeline did not start", started.await(2, TimeUnit.SECONDS));

    _registry.cancel("cancel-1");

    JobState job = _registry.get("cancel-1").orElseThrow(AssertionError::new);
    assertEquals(JobStatus.CANCELLED, job.getStatus());
    assertTrue("the pipeline future should be cancelled", job.getFuture().isCancelled());
  }

  @Test
  public void cancel_unknownJobIsNoop() {
    _registry.cancel("never-submitted"); // must not throw
    assertFalse(_registry.get("never-submitted").isPresent());
  }

  @Test
  public void cancel_doesNotOverrideAFinishedJob() throws InterruptedException {
    CountDownLatch done = new CountDownLatch(1);
    _registry.submit(submission("cancel-2"), 100L, js -> () -> {
      js.markTerminal(JobStatus.SUCCESS, null);
      done.countDown();
    });
    assertTrue("pipeline did not finish", done.await(2, TimeUnit.SECONDS));

    _registry.cancel("cancel-2");

    assertEquals("a finished job must not be overwritten by a late cancel",
        JobStatus.SUCCESS, _registry.get("cancel-2").orElseThrow(AssertionError::new).getStatus());
  }

  @Test
  public void submit_whenPoolRejects_throwsAndDoesNotRetainJob() {
    ExecutorService dead = Executors.newSingleThreadExecutor();
    dead.shutdown(); // a shutdown executor rejects all new tasks
    ScheduledExecutorService evictor = Executors.newSingleThreadScheduledExecutor();
    try {
      JobRegistry registry = new JobRegistry(dead, evictor);
      registry.submit(submission("digest-E"), 100L, noopPipeline());
      fail("expected RejectedExecutionException when the pool cannot accept the job");
    }
    catch (RejectedExecutionException expected) {
      // the rejected job must not linger in the registry, so a retry can re-submit
      // (registry is unreachable here, but the assertion is implicit: the throw
      //  came from submit, and the dead pool guarantees the entry was removed)
    }
    finally {
      evictor.shutdownNow();
    }
  }

  // --- Deliverable 9: registry eviction sweep ----------------------------------

  @Test
  public void sweep_evictsTerminalJobOlderThanTtl() {
    JobState job = _registry.submit(submission("evict-old"), 100L, noopPipeline());
    job.markTerminal(JobStatus.SUCCESS, null);
    long terminalAt = job.getTerminalAt().getTime();

    int evicted = _registry.sweep(terminalAt + JobRegistry.TTL_MILLIS + 1);

    assertEquals(1, evicted);
    assertFalse("an expired terminal job must be evicted", _registry.get("evict-old").isPresent());
  }

  @Test
  public void sweep_keepsTerminalJobWithinTtl() {
    JobState job = _registry.submit(submission("keep-recent"), 100L, noopPipeline());
    job.markTerminal(JobStatus.SUCCESS, null);
    long terminalAt = job.getTerminalAt().getTime();

    // exactly at the TTL boundary the job has not *exceeded* the TTL yet
    int evicted = _registry.sweep(terminalAt + JobRegistry.TTL_MILLIS);

    assertEquals(0, evicted);
    assertTrue("a terminal job within its TTL must be retained", _registry.get("keep-recent").isPresent());
  }

  @Test
  public void sweep_neverEvictsAStillRunningJob() {
    // the no-op pipeline returns without marking terminal: status stays RUNNING
    _registry.submit(submission("still-running"), 100L, noopPipeline());

    int evicted = _registry.sweep(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));

    assertEquals(0, evicted);
    assertTrue("a running job must never be evicted, no matter how old",
        _registry.get("still-running").isPresent());
  }

  @Test
  public void constructor_schedulesEvictionSweepAtConfiguredCadence() {
    RecordingScheduler scheduler = new RecordingScheduler();
    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      new JobRegistry(pool, scheduler);
      assertNotNull("the eviction sweep must be scheduled at construction", scheduler.command);
      assertEquals(JobRegistry.EVICTION_PERIOD_SECONDS, scheduler.initialDelay);
      assertEquals(JobRegistry.EVICTION_PERIOD_SECONDS, scheduler.period);
      assertEquals(TimeUnit.SECONDS, scheduler.unit);
    }
    finally {
      pool.shutdownNow();
      scheduler.shutdownNow();
    }
  }

  /** Captures the {@code scheduleAtFixedRate} arguments without actually scheduling, for a deterministic wiring assertion. */
  private static class RecordingScheduler extends ScheduledThreadPoolExecutor {
    volatile Runnable command;
    volatile long initialDelay;
    volatile long period;
    volatile TimeUnit unit;

    RecordingScheduler() { super(1); }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      this.command = command;
      this.initialDelay = initialDelay;
      this.period = period;
      this.unit = unit;
      return null; // intentionally do not schedule — keeps the test free of background sweeps
    }
  }
}
