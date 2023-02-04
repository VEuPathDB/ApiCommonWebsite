package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;

public class GenomicSequenceFeatureProvider implements BedFeatureProvider {

  private enum Anchor {
    DownstreamFromStart,
    UpstreamFromEnd;
  }

  private static final String ATTR_FORMATTED_LENGTH = "formatted_length";
  private static final String ATTR_ORGANISM = "organism";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final StrandDirection _strand;
  private final int _startOffset;
  private final Anchor _startAnchor;
  private final int _endOffset;
  private final Anchor _endAnchor;

  public GenomicSequenceFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _strand = StrandDirection.valueOf(config.getString("strand"));
    _startOffset = config.getInt("startOffset");
    _startAnchor = Anchor.valueOf(config.getString("startAnchor"));
    _endOffset = config.getInt("endOffset");
    _endAnchor = Anchor.valueOf(config.getString("endAnchor"));
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
    Integer segmentStart = getPosition(featureLength, _startOffset, _startAnchor);
    Integer segmentEnd = getPosition(featureLength, _endOffset, _endAnchor);

    String formattedId = String.format("%s:%s..%s(%s)", chrom, segmentStart, segmentEnd, _strand.getSign());

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
      defline.appendGenomicSequenceRangeUiChoice(
        "Sequence Region",
        getPositionDesc(_startOffset, _startAnchor),
        getPositionDesc(_endOffset, _endAnchor),
        _strand
      );
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }
    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, _strand));
  }

  private static Integer getPosition(Integer featureLength, int offset, Anchor anchor) throws WdkModelException {
    switch(anchor){
      case DownstreamFromStart: return 1 + offset;
      case UpstreamFromEnd: return featureLength - offset;
      default: throw new WdkModelException("Unsupported anchor type: " + anchor);
    }
  }

  private static String getPositionDesc(int offset, Anchor anchor) throws WdkModelException {
    StringBuilder sb = new StringBuilder();
    switch(anchor){
      case DownstreamFromStart:
        sb.append("Start");
        break;
      case UpstreamFromEnd:
        sb.append("End");
        break;
      default: throw new WdkModelException("Unsupported anchor type: " + anchor);
    }
    if(offset > 0){
      switch(anchor){
      case DownstreamFromStart:
        sb.append("+" + offset);
        break;
      case UpstreamFromEnd:
        sb.append("-" + offset);
        break;
        default: throw new WdkModelException("Unsupported anchor type: " + anchor);
      }
    }
    return sb.toString();
  }
}
