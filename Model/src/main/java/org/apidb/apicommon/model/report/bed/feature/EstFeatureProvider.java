package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;

public class EstFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_LENGTH = "length";
  private static final String ATTR_ORGANISM_TEXT = "organism_text";
  private static final String ATTR_DBEST_NAME = "dbest_name";

  private final RequestedDeflineFields _requestedDeflineFields;

  public EstFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "EstRecordClasses.EstRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_LENGTH, ATTR_ORGANISM_TEXT, ATTR_DBEST_NAME};
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    String featureId = getSourceId(record);
    String chrom = featureId;
    StrandDirection strand = StrandDirection.none;
    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM_TEXT);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendRecordAttribute(record, ATTR_DBEST_NAME);
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendValue("whole EST");
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }

    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
  }
}
