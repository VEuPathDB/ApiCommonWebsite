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

  // 1,494 of 6,119 strain names carrying indel data (24%) contain an underscore.
  // An earlier strain group of [^:_]+ rejected every one of them.
  @Test
  public void strainNameWithUnderscoreSurvives() {
    String sourceId = "Af293_resequence2:Chr1_A_fumigatus_Af293:1-100000:f";
    StrainSegmentId id = StrainSegmentId.parse(sourceId);
    assertEquals("Af293_resequence2", id.getStrain());
    assertEquals("Chr1_A_fumigatus_Af293", id.getRefSeq());
    assertEquals(1, id.getRefStart());
    assertEquals(100000, id.getRefEnd());
    assertEquals(StrandDirection.forward, id.getStrand());
    assertEquals(sourceId, id.format());
  }

  @Test
  public void strainNameWithMultipleUnderscoresSurvives() {
    String sourceId = "USGS_28834_1_NV:bcin_chr_1:5-50:r";
    StrainSegmentId id = StrainSegmentId.parse(sourceId);
    assertEquals("USGS_28834_1_NV", id.getStrain());
    assertEquals("bcin_chr_1", id.getRefSeq());
    assertEquals(5, id.getRefStart());
    assertEquals(50, id.getRefEnd());
    assertEquals(StrandDirection.reverse, id.getStrand());
    assertEquals(sourceId, id.format());
  }

  @Test
  public void strainNameWithUnderscoreAndHyphenSurvives() {
    String sourceId = "AFIS_13708_CDC-14:Chr1_A_fumigatus_Af293:100-200:f";
    StrainSegmentId id = StrainSegmentId.parse(sourceId);
    assertEquals("AFIS_13708_CDC-14", id.getStrain());
    assertEquals("Chr1_A_fumigatus_Af293", id.getRefSeq());
    assertEquals(sourceId, id.format());
  }

  @Test
  public void strainNameOfOnlyDigitsAndUnderscoresSurvives() {
    StrainSegmentId id = StrainSegmentId.parse("1_01_01:bcin_chr_1:1-100000:f");
    assertEquals("1_01_01", id.getStrain());
    assertEquals("bcin_chr_1", id.getRefSeq());
  }

  // getStrainSeqId is an opaque, one-way lookup key: a plain concatenation, even when
  // both sides contain underscores. It is never split back apart.
  @Test
  public void strainSeqIdConcatenatesUnderscoreContainingStrain() {
    assertEquals("Af293_resequence2_Chr1_A_fumigatus_Af293",
        StrainSegmentId.parse("Af293_resequence2:Chr1_A_fumigatus_Af293:1-100000:f")
            .getStrainSeqId());
    assertEquals("USGS_28834_1_NV_bcin_chr_1",
        StrainSegmentId.parse("USGS_28834_1_NV:bcin_chr_1:5-50:r").getStrainSeqId());
  }

  // ':' is the one true delimiter, so an extra one is still ambiguous and still rejected.
  @Test
  public void rejectsColonInStrainName() {
    assertRejected("Af293:resequence2:Chr1_A_fumigatus_Af293:1-100000:f");
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

  @Test
  public void acceptsSingleBaseSegment() {
    StrainSegmentId id = StrainSegmentId.parse("A0003:AACB03000001:100-100:f");
    assertEquals(100, id.getRefStart());
    assertEquals(100, id.getRefEnd());
  }

  @Test
  public void rejectsCoordinateBelowOne() {
    assertRejected("A0003:AACB03000001:0-200:f");
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
