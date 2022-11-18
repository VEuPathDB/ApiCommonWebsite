package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.report.bed.BedGenomicSequenceReporter.StrandDirection;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;

public class GenomicGenomicFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_FORMATTED_LENGTH = "formatted_length";
  private static final String ATTR_ORGANISM = "organism";

  private final boolean _useShortDefline;
  private final StrandDirection _longStrand;

  public GenomicGenomicFeatureProvider(JSONObject config) {
    _useShortDefline = useShortDefline(config);
    _longStrand = StrandDirection.valueOf(config.getString("strand"));
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "SequenceRecordClasses.SequenceRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
        ATTR_FORMATTED_LENGTH,
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
    String chrom = featureId;
    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_FORMATTED_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline) {
      defline.append("  | ");
      defline.append(stringValue(record, ATTR_ORGANISM));
      defline.append(" | genomic sequence | ");
      defline.append(chrom);
      defline.append(", ");
      defline.append(_longStrand.name() + " strand");
      defline.append(" | segment_length=");
      defline.append(""+featureLength);
    }

    return List.of(List.of(chrom, segmentStart.toString(), segmentEnd.toString(), defline.toString(), ".", _longStrand.getSign()));
  }

}
