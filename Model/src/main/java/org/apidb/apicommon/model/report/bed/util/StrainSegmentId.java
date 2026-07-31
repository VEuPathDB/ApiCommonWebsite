package org.apidb.apicommon.model.report.bed.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The single parse/format authority for the strain genomic segment primary key.
 *
 * Grammar: {@code <strain>:<refSeq>:<refStart>-<refEnd>:<f|r>}
 * Example: {@code A0003:AACB03000001:100-200:f}
 *
 * Coordinates are 1-based, inclusive, and expressed in REFERENCE coordinates.
 * Strain coordinates are derived by the attribute query, not stored here.
 *
 * Note the field patterns are {@code [^:]+} rather than DynSpan's greedy {@code (.*)}.
 * DynSpan's SQL and Java parsers disagree on IDs containing extra colons; this one
 * rejects them instead of guessing.
 */
public class StrainSegmentId {

  private static final Pattern PATTERN =
      Pattern.compile("^([^:_]+):([^:]+):(\\d+)-(\\d+):(f|r)$");

  private final String _strain;
  private final String _refSeq;
  private final int _refStart;
  private final int _refEnd;
  private final StrandDirection _strand;

  private StrainSegmentId(String sourceId, String strain, String refSeq, int refStart, int refEnd,
      StrandDirection strand) {
    if (refStart < 1) {
      throw new IllegalArgumentException(String.format(
          "Strain segment ID '%s' has refStart %d, which is less than the minimum of 1",
          sourceId, refStart));
    }
    if (refEnd < refStart) {
      throw new IllegalArgumentException(String.format(
          "Strain segment ID '%s' has end %d less than start %d", sourceId, refEnd, refStart));
    }
    if (!StrandDirection.forward.equals(strand) && !StrandDirection.reverse.equals(strand)) {
      throw new IllegalArgumentException(String.format(
          "Strain segment ID '%s' has strand %s, which must be forward or reverse",
          sourceId, strand));
    }
    _strain = strain;
    _refSeq = refSeq;
    _refStart = refStart;
    _refEnd = refEnd;
    _strand = strand;
  }

  public static StrainSegmentId parse(String sourceId) {
    if (sourceId == null) {
      throw new IllegalArgumentException("Strain segment ID may not be null");
    }
    Matcher m = PATTERN.matcher(sourceId);
    if (!m.matches()) {
      throw new IllegalArgumentException(String.format(
          "Strain segment ID '%s' does not match required pattern %s",
          sourceId, PATTERN.pattern()));
    }
    int start = parseCoordinate(sourceId, m.group(3));
    int end = parseCoordinate(sourceId, m.group(4));
    return new StrainSegmentId(sourceId, m.group(1), m.group(2), start, end,
        StrandDirection.fromEfOrEr(m.group(5)));
  }

  private static int parseCoordinate(String sourceId, String coordinate) {
    try {
      return Integer.parseInt(coordinate);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format(
          "Strain segment ID '%s' has non-numeric or out-of-range coordinate '%s'",
          sourceId, coordinate), e);
    }
  }

  /**
   * Returns the canonical form of this ID, equal to the input for canonical input
   * (e.g. an input with leading zeros in its coordinates re-formats without them).
   */
  public String format() {
    return String.format("%s:%s:%d-%d:%s", _strain, _refSeq, _refStart, _refEnd,
        StrandDirection.reverse.equals(_strand) ? "r" : "f");
  }

  /**
   * The key into the strain consensus FASTA, and therefore the BED chrom column.
   * Written by dnaseq-nextflow as {@code <sample>_<chrom>}.
   *
   * The strain field is constrained to contain no underscore (see PATTERN) because
   * reference sequence IDs legitimately do (e.g. {@code Pf3D7_01_v3}); without that
   * constraint, two different (strain, refSeq) pairs could concatenate to the same
   * ambiguous key.
   */
  public String getStrainSeqId() {
    return _strain + "_" + _refSeq;
  }

  public String getStrain() { return _strain; }
  public String getRefSeq() { return _refSeq; }
  public int getRefStart() { return _refStart; }
  public int getRefEnd() { return _refEnd; }
  public StrandDirection getStrand() { return _strand; }
}
