package org.apidb.apicommon.model.report.bed;

import java.util.List;

import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

// BedEstReporter: the best reporter!
public class BedEstReporter extends BedReporter implements BedFeatureProvider {

  private static final String ATTR_LENGTH = "length";
  private static final String ATTR_ORGANISM_TEXT = "organism_text";
  private static final String ATTR_DBEST_NAME = "dbest_name";

  private boolean _useShortDefline;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    _useShortDefline = useShortDefline(config);
    return configure(this);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "EstRecordClasses.EstRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_LENGTH };
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
    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline) {
      defline.append("  | ");
      defline.append(stringValue(record, ATTR_ORGANISM_TEXT));
      defline.append(" | EST | ");
      defline.append(stringValue(record, ATTR_DBEST_NAME));
      defline.append(" | segment_length=");
      defline.append(""+featureLength);
    }

    return List.of(List.of(chrom, "" + segmentStart, "" + segmentEnd, defline.toString(), ".", strand));
  }
}
