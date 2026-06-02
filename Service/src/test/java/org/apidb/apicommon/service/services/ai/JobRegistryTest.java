package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JobRegistryTest {

  private ExecutorService _executor;
  private JobRegistry _registry;

  @Before
  public void setUp() {
    _executor = Executors.newSingleThreadExecutor();
    _registry = new JobRegistry(_executor, Executors.newSingleThreadScheduledExecutor());
  }

  @After
  public void tearDown() {
    _executor.shutdownNow();
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
  public void submit_whenPoolRejects_throwsAndDoesNotRetainJob() {
    ExecutorService dead = Executors.newSingleThreadExecutor();
    dead.shutdown(); // a shutdown executor rejects all new tasks
    JobRegistry registry = new JobRegistry(dead, Executors.newSingleThreadScheduledExecutor());
    try {
      registry.submit(submission("digest-E"), 100L, noopPipeline());
      fail("expected RejectedExecutionException when the pool cannot accept the job");
    }
    catch (RejectedExecutionException expected) {
      // the rejected job must not linger in the registry, so a retry can re-submit
      assertFalse(registry.get("digest-E").isPresent());
    }
  }
}
