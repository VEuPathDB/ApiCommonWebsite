package org.apidb.apicommon.model.report.bed.feature;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;

/**
 * Covers the strain-coordinate validation the provider adds on top of
 * {@link org.apidb.apicommon.model.report.bed.util.StrainSegmentId}, which validates only
 * the REFERENCE coordinates in the primary key and knows nothing about strain coordinates.
 *
 * Only the static seam is exercised: getRecordAsBedFields needs a RecordInstance, which
 * needs a loaded WDK model, so the record-reading path is covered end to end instead
 * (spec section 9 / plan Task 7).
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
   * The live case from spec 5.3.2: a 1-bp segment on a shift = -54 deletion event.  The
   * attribute query deliberately leaves these unclamped, so this is the only thing standing
   * between an inverted interval and a FASTA lookup that would return wrong sequence.
   */
  @Test
  public void rejectsInvertedIntervalInsideADeletion() {
    try {
      StrainSegmentFeatureProvider.validateStrainInterval(
          "366.1:Pf3D7_10_v3:331757-331757:f", "366.1_Pf3D7_10_v3", 331658, 331604);
      fail("expected an inverted strain interval to be rejected");
    }
    catch (WdkModelException e) {
      // the message must name the primary key and BOTH coordinates
      assertTrue(e.getMessage(), e.getMessage().contains("366.1:Pf3D7_10_v3:331757-331757:f"));
      assertTrue(e.getMessage(), e.getMessage().contains("331658"));
      assertTrue(e.getMessage(), e.getMessage().contains("331604"));
    }
  }

  @Test
  public void rejectsOffByOneInversion() {
    try {
      StrainSegmentFeatureProvider.validateStrainInterval(
          "A0003:AACB03000001:100-200:f", "A0003_AACB03000001", 143, 142);
      fail("expected an inverted strain interval to be rejected");
    }
    catch (WdkModelException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("inverted"));
    }
  }

  /** A start below 1 would reach BedLine.locationToZeroBased and emit chromStart = -1. */
  @Test
  public void rejectsStartBelowOne() {
    try {
      StrainSegmentFeatureProvider.validateStrainInterval(
          "A0003:AACB03000001:100-200:f", "A0003_AACB03000001", 0, 241);
      fail("expected a strain_start below 1 to be rejected");
    }
    catch (WdkModelException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("A0003:AACB03000001:100-200:f"));
      assertTrue(e.getMessage(), e.getMessage().contains("minimum of 1"));
    }
  }

  @Test
  public void rejectsNegativeStart() {
    try {
      StrainSegmentFeatureProvider.validateStrainInterval(
          "A0003:AACB03000001:100-200:f", "A0003_AACB03000001", -5, 241);
      fail("expected a negative strain_start to be rejected");
    }
    catch (WdkModelException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("-5"));
    }
  }
}
