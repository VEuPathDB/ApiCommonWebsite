package org.apidb.apicommon.model.report.bed;

import java.util.List;

import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class BedPopsetReporter extends BedReporter implements BedFeatureProvider {

  private static final String ATTR_SEQUENCE_LENGTH = "sequence_length";
  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_DESCRIPTION = "description";

  private boolean _useShortDefline;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    _useShortDefline = useShortDefline(config);
    return configure(this);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "PopsetRecordClasses.PopsetRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
        ATTR_SEQUENCE_LENGTH,
        ATTR_ORGANISM,
        ATTR_DESCRIPTION
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
    String strand = ".";

    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_SEQUENCE_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline) {
      defline.append("  | ");
      defline.append(stringValue(record, ATTR_ORGANISM));
      defline.append(" | popset | ");
      defline.append(stringValue(record, ATTR_DESCRIPTION));
      defline.append(" | segment_length=");
      defline.append(""+featureLength);
    }

    return List.of(List.of(chrom, "" + segmentStart, "" + segmentEnd, defline.toString(), ".", strand));
  }
}
