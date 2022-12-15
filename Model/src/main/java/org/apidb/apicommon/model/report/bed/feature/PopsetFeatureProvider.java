package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;

public class PopsetFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_SEQUENCE_LENGTH = "sequence_length";
  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_DESCRIPTION = "description";

  private final RequestedDeflineFields _requestedDeflineFields;

  public PopsetFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
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
    StrandDirection strand = StrandDirection.none;

    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_SEQUENCE_LENGTH).replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendRecordAttribute(record, ATTR_DESCRIPTION);
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendValue("whole popset");
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }

    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
  }
}
