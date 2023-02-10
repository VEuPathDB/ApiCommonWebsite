package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;

public class ProteinTableFieldFeatureProvider extends TableFieldFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final String _tableFieldName;

  public ProteinTableFieldFeatureProvider(JSONObject config,
      String tableFieldName, String startTableAttributeName, String endTableAttributeName) {
    super(tableFieldName, startTableAttributeName, endTableAttributeName);
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _tableFieldName = tableFieldName;

  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.GENE_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM };
  }

  @Override
  protected List<String> createFeatureRow(RecordInstance record, Map<String, AttributeValue> tableRow, Integer segmentStart, Integer segmentEnd) throws WdkModelException {
    String chrom = tableRow.get("transcript_id").toString();
    StrandDirection strand = StrandDirection.none;

    String featureId = chrom + "::" + segmentStart + "-" + segmentEnd;
    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendValue("protein");
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPositionAa(chrom, segmentStart, segmentEnd);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendValue("protein features: " + _tableFieldName);
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }
    return BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand);
  }
}
