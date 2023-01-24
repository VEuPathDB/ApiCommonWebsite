package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public class GenomicTableFieldFeatureProvider extends TableFieldFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";

  private final StrandDirection _strand;
  private final RequestedDeflineFields _requestedDeflineFields;
  private final String _featureNamePretty;

  public GenomicTableFieldFeatureProvider(JSONObject config,
      String tableFieldName, String featureNamePretty, String startTableAttributeName, String endTableAttributeName) {
    super(tableFieldName, startTableAttributeName, endTableAttributeName);
    _featureNamePretty = featureNamePretty;
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _strand = StrandDirection.valueOf(config.getString("strand"));
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM };
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "SequenceRecordClasses.SequenceRecordClass";
  }

  @Override
  protected List<String> createFeatureRow(RecordInstance record, Map<String, AttributeValue> tableRow,
      Integer segmentStart, Integer segmentEnd) throws WdkModelException {
    String sourceId = getSourceId(record);
    String chrom = sourceId;
    String formattedId = String.format("%s::%s-%s (%s)", chrom, segmentStart, segmentEnd, _strand.getSign());
    DeflineBuilder defline = new DeflineBuilder(formattedId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendValue("genomic sequence");
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, _strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendGenomicFeatureUiChoice(_featureNamePretty, _strand);
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }
    return BedLine.bed6(chrom, segmentStart, segmentEnd, defline, _strand);
  }
}
