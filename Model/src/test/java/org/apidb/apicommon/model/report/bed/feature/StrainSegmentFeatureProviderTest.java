package org.apidb.apicommon.model.report.bed.feature;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apidb.apicommon.model.report.bed.util.StrainSegmentId;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;

/**
 * Covers the validation the provider adds on top of
 * {@link org.apidb.apicommon.model.report.bed.util.StrainSegmentId}, which validates only
 * the REFERENCE coordinates in the primary key and knows nothing about strain coordinates
 * or about the strain_seq_id attribute.
 *
 * Only the static seams are exercised: getRecordAsBedFields needs a RecordInstance, which
 * needs a loaded WDK model, so the record-reading path is to be covered end to end by
 * plan Task 7 (spec section 9), which has not run yet.
 */
public class StrainSegmentFeatureProviderTest {

  @Test
  public void acceptsForwardInterval() throws Exception {
    StrainSegmentFeatureProvider.validateStrainInterval(
        "A0003:AACB03000001:100-200:f", "A0003_AACB03000001", 143, 241);
  }

  @Test
  public void acceptsSingleBaseInterval() throws Exception {
    StrainSegmentFeatureProvider.validateStrainInterval(
        "A0003:AACB03000001:100-100:f", "A0003_AACB03000001", 143, 143);
  }

  /**
   * The accept side of the strainStart boundary.  Without this, '<' silently becoming '<='
   * still passes every reject test while breaking every segment that starts at position 1
   * of a contig - which is a real case, not a corner one.
   */
  @Test
  public void acceptsStartAtExactlyOne() throws Exception {
    StrainSegmentFeatureProvider.validateStrainInterval(
        "A0003:AACB03000001:1-1:f", "A0003_AACB03000001", 1, 1);
  }

  /**
   * The live case from spec 5.3.2: a 1-bp segment on a shift = -54 deletion event.  The
   * attribute query deliberately leaves these unclamped, so this is the only thing standing
   * between an inverted interval and a FASTA lookup that would return wrong sequence.
   *
   * The message must name the primary key and BOTH coordinates.
   */
  @Test
  public void rejectsInvertedIntervalInsideADeletion() {
    assertIntervalRejected("366.1:Pf3D7_10_v3:331757-331757:f", "366.1_Pf3D7_10_v3",
        331658, 331604, "366.1:Pf3D7_10_v3:331757-331757:f", "331658", "331604");
  }

  @Test
  public void rejectsOffByOneInversion() {
    assertIntervalRejected("A0003:AACB03000001:100-200:f", "A0003_AACB03000001",
        143, 142, "inverted");
  }

  /** A start below 1 would reach BedLine.locationToZeroBased and emit chromStart = -1. */
  @Test
  public void rejectsStartBelowOne() {
    assertIntervalRejected("A0003:AACB03000001:100-200:f", "A0003_AACB03000001",
        0, 241, "A0003:AACB03000001:100-200:f", "minimum of 1");
  }

  @Test
  public void rejectsNegativeStart() {
    assertIntervalRejected("A0003:AACB03000001:100-200:f", "A0003_AACB03000001",
        -5, 241, "-5");
  }

  // ---- the chrom / primary key cross-check -------------------------------------------

  @Test
  public void acceptsStrainSeqIdMatchingThePrimaryKey() throws Exception {
    String featureId = "Af293_resequence2:Chr1_A_fumigatus_Af293:100-200:f";
    StrainSegmentFeatureProvider.validateStrainSeqIdMatchesId(
        featureId, StrainSegmentId.parse(featureId),
        "Af293_resequence2_Chr1_A_fumigatus_Af293");
  }

  /**
   * The failure this guards: the attribute query sourcing the strain from somewhere other
   * than the primary key, so chrom names a contig that is not the one the ID describes.
   */
  @Test
  public void rejectsStrainSeqIdThatDisagreesWithThePrimaryKey() {
    String featureId = "A0003:AACB03000001:100-200:f";
    try {
      StrainSegmentFeatureProvider.validateStrainSeqIdMatchesId(
          featureId, StrainSegmentId.parse(featureId), "A0004_AACB03000001");
      fail("expected a strain_seq_id disagreeing with the primary key to be rejected");
    }
    catch (WdkModelException e) {
      // the message must name the primary key and BOTH keys
      assertContains(e, featureId);
      assertContains(e, "A0004_AACB03000001");
      assertContains(e, "A0003_AACB03000001");
    }
  }

  /** An empty attribute value must not pass as "close enough" to the computed key. */
  @Test
  public void rejectsEmptyStrainSeqId() {
    String featureId = "A0003:AACB03000001:100-200:f";
    try {
      StrainSegmentFeatureProvider.validateStrainSeqIdMatchesId(
          featureId, StrainSegmentId.parse(featureId), "");
      fail("expected an empty strain_seq_id to be rejected");
    }
    catch (WdkModelException e) {
      assertContains(e, "A0003_AACB03000001");
    }
  }

  /**
   * Asserts validateStrainInterval rejects these coordinates, and that the exception message
   * contains every expected fragment - a message that dropped the primary key or a
   * coordinate must still fail the test, not just the absence of an exception.
   */
  private static void assertIntervalRejected(String featureId, String strainSeqId,
      int strainStart, int strainEnd, String... expectedInMessage) {
    try {
      StrainSegmentFeatureProvider.validateStrainInterval(
          featureId, strainSeqId, strainStart, strainEnd);
      fail(String.format("expected strain interval %d-%d on '%s' to be rejected for '%s'",
          strainStart, strainEnd, strainSeqId, featureId));
    }
    catch (WdkModelException e) {
      for (String expected : expectedInMessage) {
        assertContains(e, expected);
      }
    }
  }

  private static void assertContains(WdkModelException e, String expected) {
    assertTrue("expected message to contain '" + expected + "', was: " + e.getMessage(),
        e.getMessage().contains(expected));
  }
}
