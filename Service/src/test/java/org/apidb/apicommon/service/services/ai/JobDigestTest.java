package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class JobDigestTest {

  private static final String OPTS = "{\"create_user_comment\":true,\"generate_product_description\":false,\"validate\":true}";

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
    String optsB = "{\"create_user_comment\":true,\"generate_product_description\":true,\"validate\":true}";
    assertNotEquals(
        JobDigest.compute("g1", Collections.emptyList(), "src", "m", "p", OPTS),
        JobDigest.compute("g1", Collections.emptyList(), "src", "m", "p", optsB));
  }

  @Test
  public void canonicalOptionsJson_defaults_sortedKeysNoWhitespace() {
    AiGenePublicationRequest.Options options = new AiGenePublicationRequest.Options();
    // defaults: validate=true, generate_product_description=false, create_user_comment=true
    assertEquals(
        "{\"create_user_comment\":true,\"generate_product_description\":false,\"validate\":true}",
        JobDigest.canonicalOptionsJson(options));
  }

  @Test
  public void canonicalOptionsJson_reflectsNonDefaultValues() {
    AiGenePublicationRequest.Options options = new AiGenePublicationRequest.Options();
    options.validate = false;
    options.generateProductDescription = true;
    options.createUserComment = false;
    assertEquals(
        "{\"create_user_comment\":false,\"generate_product_description\":true,\"validate\":false}",
        JobDigest.canonicalOptionsJson(options));
  }
}
