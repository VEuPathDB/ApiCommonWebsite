package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;

public class ProteinSequenceFeatureProvider implements BedFeatureProvider {

  private enum ProteinAnchor {
    DownstreamFromStart,
    UpstreamFromEnd
  }

  private static final String ATTR_PROTEIN_LENGTH = "protein_length";
  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_GENE_PRODUCT = "gene_product";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final int _startOffset;
  private final ProteinAnchor _startAnchor;
  private final int _endOffset;
  private final ProteinAnchor _endAnchor;

  public ProteinSequenceFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);

    _startOffset = config.getInt("startOffset3");
    _startAnchor = ProteinAnchor.valueOf(config.getString("startAnchor3"));

    _endOffset = config.getInt("endOffset3");
    _endAnchor = ProteinAnchor.valueOf(config.getString("endAnchor3"));
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.TRANSCRIPT_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
        ATTR_PROTEIN_LENGTH,
        ATTR_ORGANISM,
        ATTR_GENE_PRODUCT
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
    Integer featureLength = Integer.valueOf(stringValue(record, ATTR_PROTEIN_LENGTH));
    StrandDirection strand = StrandDirection.none;

    Integer segmentStart = getPositionProtein(featureLength, _startOffset, _startAnchor);
    Integer segmentEnd = getPositionProtein(featureLength, _endOffset, _endAnchor);

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendRecordAttribute(record, ATTR_GENE_PRODUCT);
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPositionAa(chrom, segmentStart, segmentEnd);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      boolean reverseAndComplement = false;
      defline.appendRangeUiChoice(
        "Protein Sequence",
        getPositionDescProtein(_startOffset, _startAnchor),
        getPositionDescProtein(_endOffset, _endAnchor),
        reverseAndComplement
      );
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }

    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
  }

  private static Integer getPositionProtein(Integer featureLength, int offset, ProteinAnchor anchor) throws WdkModelException {
    switch(anchor){
      case DownstreamFromStart: return 1 + offset;
      case UpstreamFromEnd: return featureLength - offset;
      default: throw new WdkModelException("Unsupported anchor type: " + anchor);
    }
  }

  private static String getPositionDescProtein(int offset, ProteinAnchor anchor) throws WdkModelException {
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
