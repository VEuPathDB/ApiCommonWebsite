package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class JobDigestTest {

  // Product descriptions are now compulsory, so the options object carries no
  // per-request flags — the canonical form is an empty object.
  private static final String OPTS = "{}";

  @Test
  public void compute_returns64CharLowercaseHex() {
    String digest = JobDigest.compute("PF3D7_1133400", Arrays.asList("eIF4E"),
        "12345678", "claude-sonnet-4-20250514", "getGeneSummary/v1", OPTS);
    assertTrue("expected 64-char hex, got: " + digest, digest.matches("[0-9a-f]{64}"));
  }

  @Test
  public void compute_isDeterministic() {
    assertEquals(
        JobDigest.compute("g1", Arrays.asList("a", "b"), "src", "m", "p", OPTS),
        JobDigest.compute("g1", Arrays.asList("a", "b"), "src", "m", "p", OPTS));
  }

  @Test
  public void compute_isInsensitiveToSynonymOrder() {
    assertEquals(
        JobDigest.compute("g1", Arrays.asList("a", "b", "c"), "src", "m", "p", OPTS),
        JobDigest.compute("g1", Arrays.asList("c", "a", "b"), "src", "m", "p", OPTS));
  }

  @Test
  public void compute_differsWhenSourceKeyDiffers() {
    assertNotEquals(
        JobDigest.compute("g1", Collections.emptyList(), "pmid:111", "m", "p", OPTS),
        JobDigest.compute("g1", Collections.emptyList(), "pmid:222", "m", "p", OPTS));
  }

  @Test
  public void compute_differsWhenModelDiffers() {
    assertNotEquals(
        JobDigest.compute("g1", Collections.emptyList(), "src", "claude-sonnet-4-20250514", "p", OPTS),
        JobDigest.compute("g1", Collections.emptyList(), "src", "claude-sonnet-4-6", "p", OPTS));
  }

  @Test
  public void compute_differsWhenOptionsDiffer() {
    // The options JSON still participates in the digest even though no request
    // flags remain today, so a future flag automatically invalidates the cache.
    assertNotEquals(
        JobDigest.compute("g1", Collections.emptyList(), "src", "m", "p", "{}"),
        JobDigest.compute("g1", Collections.emptyList(), "src", "m", "p", "{\"future_flag\":true}"));
  }

  @Test
  public void canonicalOptionsJson_defaults_emptyObject() {
    AiGenePublicationRequest.Options options = new AiGenePublicationRequest.Options();
    // Product descriptions are compulsory (generate_product_description was
    // removed); validate was removed 2026-06-05; create_user_comment on the
    // review-on-approval pivot. No per-request options remain.
    assertEquals("{}", JobDigest.canonicalOptionsJson(options));
  }
}
