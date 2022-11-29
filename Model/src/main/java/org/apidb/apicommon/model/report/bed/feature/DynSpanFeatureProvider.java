package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;

public class DynSpanFeatureProvider implements BedFeatureProvider {

  //Pf3D7_03_v3:1-100:f
  private static final Pattern DYN_SPAN_SOURCE_ID_PATTERN = Pattern.compile("^(.*):(\\d+)-(\\d+):(f|r)$");
  private static final String ATTR_ORGANISM = "organism";

  private final RequestedDeflineFields _requestedDeflineFields;

  public DynSpanFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "DynSpanRecordClasses.DynSpanRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }



  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    String featureId = getSourceId(record);
    Matcher m = DYN_SPAN_SOURCE_ID_PATTERN.matcher(featureId);
    if (!m.matches()){
      throw new WdkModelException(String.format("Genomic segment ID %s not matching pattern %s", featureId, DYN_SPAN_SOURCE_ID_PATTERN.toString()));
    }

    String chrom = m.group(1);
    Integer segmentStart = Integer.valueOf(m.group(2));
    Integer segmentEnd = Integer.valueOf(m.group(3));
    StrandDirection strand = StrandDirection.fromEfOrEr(m.group(4));

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendValue("segment of genomic sequence");
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendValue("sequence");
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }

    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
  }
}
