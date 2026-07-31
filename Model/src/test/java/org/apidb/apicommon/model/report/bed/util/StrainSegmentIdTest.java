package org.apidb.apicommon.model.report.bed.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class StrainSegmentIdTest {

  @Test
  public void parsesForwardStrandId() {
    StrainSegmentId id = StrainSegmentId.parse("A0003:AACB03000001:100-200:f");
    assertEquals("A0003", id.getStrain());
    assertEquals("AACB03000001", id.getRefSeq());
    assertEquals(100, id.getRefStart());
    assertEquals(200, id.getRefEnd());
    assertEquals(StrandDirection.forward, id.getStrand());
  }

  @Test
  public void parsesReverseStrandId() {
    StrainSegmentId id = StrainSegmentId.parse("S7:AACB03000001:1-50:r");
    assertEquals("S7", id.getStrain());
    assertEquals(StrandDirection.reverse, id.getStrand());
  }

  // The BED chrom column must equal the dnaseq consensus FASTA defline,
  // which makeConsensusFastaFromVcfAndBed.py writes as <sample>_<chrom>.
  @Test
  public void strainSeqIdMatchesFastaKey() {
    assertEquals("A0003_AACB03000001",
        StrainSegmentId.parse("A0003:AACB03000001:100-200:f").getStrainSeqId());
  }

  // Real strain names contain hyphens; the range is a separate colon field so this is safe.
  @Test
  public void strainNameWithHyphensSurvives() {
    StrainSegmentId id = StrainSegmentId.parse("X10462-P1C9:AACB03000001:1-50:r");
    assertEquals("X10462-P1C9", id.getStrain());
    assertEquals(1, id.getRefStart());
    assertEquals(50, id.getRefEnd());
  }

  @Test
  public void formatRoundTripsBothStrands() {
    String forward = "A17-48H-7:AACB03000001:5-9:f";
    String reverse = "A17-48H-7:AACB03000001:5-9:r";
    assertEquals(forward, StrainSegmentId.parse(forward).format());
    assertEquals(reverse, StrainSegmentId.parse(reverse).format());
  }

  // DynSpan's greedy "^(.*):" regex silently mis-parses these. We reject instead.
  @Test
  public void rejectsColonInSequenceIdRatherThanMisparsing() {
    assertRejected("A0003:AAC:B03:100-200:f");
  }

  @Test
  public void rejectsMissingStrand() {
    assertRejected("A0003:AACB03000001:100-200");
  }

  @Test
  public void rejectsMissingStrain() {
    assertRejected("AACB03000001:100-200:f");
  }

  @Test
  public void rejectsStartGreaterThanEnd() {
    assertRejected("A0003:AACB03000001:200-100:f");
  }

  @Test
  public void rejectsNonNumericCoordinates() {
    assertRejected("A0003:AACB03000001:abc-200:f");
  }

  @Test
  public void rejectsBadStrandLetter() {
    assertRejected("A0003:AACB03000001:100-200:x");
  }

  @Test
  public void rejectsNull() {
    assertRejected(null);
  }

  private static void assertRejected(String sourceId) {
    try {
      StrainSegmentId.parse(sourceId);
      fail("expected IllegalArgumentException for: " + sourceId);
    }
    catch (IllegalArgumentException expected) {
      // expected
    }
  }
}
