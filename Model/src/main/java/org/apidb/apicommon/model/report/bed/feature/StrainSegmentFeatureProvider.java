package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.StrainSegmentId;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

/**
 * Emits one BED line per strain genomic segment.
 *
 * The two columns have very different constraints (spec section 7):
 *
 * - chrom is HARD: it must equal the strain consensus FASTA key ({@code <strain>_<refSeq>},
 *   written by dnaseq-nextflow as {@code <sample>_<chrom>}), or the FASTA index lookup
 *   fails.  It is taken from the {@code strain_seq_id} ATTRIBUTE rather than recomputed
 *   here, so the model stays the single source of truth for the key that was looked up.
 * - name is FREE: under {@code deflineFormat=QUERYONLY} seqret emits {@code '>' + name}
 *   verbatim, so it becomes the eventual FASTA defline.  It carries provenance, i.e. both
 *   coordinate systems.  RequestedDeflineFields is populated only when the caller passes
 *   {@code deflineType=full}, so by default the name column is the bare primary key.
 *
 * Reference coordinates are parsed from the primary key by {@link StrainSegmentId}, which
 * already validates {@code refStart >= 1}, {@code refEnd >= refStart} and forward/reverse
 * strand.  The STRAIN coordinates it knows nothing about, so they are validated here: they
 * arrive deliberately UNCLAMPED from {@code StrainSegmentAttributes.Coords} (spec 5.3.2),
 * so a segment lying inside a deletion reports {@code strain_end < strain_start}.  Nothing
 * upstream rejects that, and an inverted interval reaching a FASTA lookup is a silent wrong
 * answer, so it must fail loudly here.
 */
public class StrainSegmentFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_STRAIN_SEQ_ID = "strain_seq_id";
  private static final String ATTR_STRAIN_START = "strain_start";
  private static final String ATTR_STRAIN_END = "strain_end";
  private static final String ATTR_ORGANISM = "organism";

  private final RequestedDeflineFields _requestedDeflineFields;

  public StrainSegmentFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "StrainSegmentRecordClasses.StrainSegmentRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    // strain and the reference range come from the primary key, so they need no attribute
    return new String[] {
        ATTR_STRAIN_SEQ_ID,
        ATTR_STRAIN_START,
        ATTR_STRAIN_END,
        ATTR_ORGANISM
    };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    String featureId = getSourceId(record);

    StrainSegmentId id;
    try {
      id = StrainSegmentId.parse(featureId);
    }
    catch (IllegalArgumentException e) {
      throw new WdkModelException(e.getMessage(), e);
    }

    // chrom must be the FASTA key; take it from the attribute rather than recomputing it,
    // so the model stays the single source of truth for what was looked up.
    String strainSeqId = requiredStringAttribute(record, ATTR_STRAIN_SEQ_ID, featureId);
    int strainStart = requiredIntegerAttribute(record, ATTR_STRAIN_START, featureId);
    int strainEnd = requiredIntegerAttribute(record, ATTR_STRAIN_END, featureId);

    validateStrainInterval(featureId, strainSeqId, strainStart, strainEnd);

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if (_requestedDeflineFields.contains("organism")) {
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if (_requestedDeflineFields.contains("strain")) {
      defline.appendValue(id.getStrain());
    }
    if (_requestedDeflineFields.contains("description")) {
      defline.appendValue("segment of strain genomic sequence");
    }
    if (_requestedDeflineFields.contains("reference_position")) {
      defline.appendPosition(id.getRefSeq(), id.getRefStart(), id.getRefEnd(), id.getStrand());
    }
    if (_requestedDeflineFields.contains("position")) {
      defline.appendPosition(strainSeqId, strainStart, strainEnd, id.getStrand());
    }
    if (_requestedDeflineFields.contains("segment_length")) {
      defline.appendSegmentLength(strainStart, strainEnd);
    }

    // bed6 converts start to 0-based itself, so pass 1-based coordinates
    return List.of(BedLine.bed6(strainSeqId, strainStart, strainEnd, defline, id.getStrand()));
  }

  /**
   * Rejects a strain interval that cannot be a BED feature.  Extracted as a static method
   * with no WDK plumbing so it is directly unit testable (constructing a RecordInstance
   * requires a loaded model); see StrainSegmentFeatureProviderTest.
   *
   * Two failure modes, both silent if not caught here:
   * - start below 1 would reach BedLine.locationToZeroBased and emit chromStart = -1;
   * - end below start is the inverted interval of spec 5.3.2 - the segment lies inside a
   *   deletion, so the reference bases do not exist in this strain and the range maps to
   *   nothing.  Left unclamped by the attribute query precisely so this check can see it.
   */
  static void validateStrainInterval(String featureId, String strainSeqId, int strainStart,
      int strainEnd) throws WdkModelException {
    if (strainStart < 1) {
      throw new WdkModelException(String.format(
          "Strain segment '%s' maps to strain coordinates %d-%d on '%s': strain_start %d is" +
          " less than the minimum of 1, which would emit a negative BED chromStart.",
          featureId, strainStart, strainEnd, strainSeqId, strainStart));
    }
    if (strainEnd < strainStart) {
      throw new WdkModelException(String.format(
          "Strain segment '%s' maps to the inverted strain interval %d-%d on '%s':" +
          " strain_end %d is less than strain_start %d, which means the requested reference" +
          " range lies inside a deletion in this strain and has no sequence.  Refusing to" +
          " emit a malformed BED feature.",
          featureId, strainStart, strainEnd, strainSeqId, strainEnd, strainStart));
    }
  }

  /**
   * Reads a required attribute as a non-empty string.  AttributeValue.toString() renders a
   * NULL column value as the empty string, so "absent" and "empty" are indistinguishable
   * downstream; both are a defect here, and both name the primary key and the attribute.
   */
  private static String requiredStringAttribute(RecordInstance record, String key, String featureId)
      throws WdkModelException {
    AttributeValue value;
    try {
      value = record.getAttributeValue(key);
    }
    catch (WdkUserException e) {
      throw new WdkModelException(String.format(
          "Strain segment '%s': could not read required attribute '%s'", featureId, key), e);
    }
    String stringValue = value == null ? null : value.toString();
    if (stringValue == null || stringValue.trim().isEmpty()) {
      throw new WdkModelException(String.format(
          "Strain segment '%s' has no value for required attribute '%s'", featureId, key));
    }
    return stringValue.trim();
  }

  /**
   * Reads a required attribute as an int.  Deliberately NOT
   * {@code integerValueWithZeroForEmpty}: that maps a missing value to 0, which would then
   * be reported as an out-of-range coordinate rather than as the missing attribute it is.
   */
  private static int requiredIntegerAttribute(RecordInstance record, String key, String featureId)
      throws WdkModelException {
    String stringValue = requiredStringAttribute(record, key, featureId);
    try {
      return Integer.parseInt(stringValue);
    }
    catch (NumberFormatException e) {
      throw new WdkModelException(String.format(
          "Strain segment '%s' has non-numeric or out-of-range value '%s' for required" +
          " integer attribute '%s'", featureId, stringValue, key), e);
    }
  }
}
