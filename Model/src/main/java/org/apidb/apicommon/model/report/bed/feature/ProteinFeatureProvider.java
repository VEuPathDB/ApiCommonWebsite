package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONObject;

public class ProteinFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_PROTEIN_LENGTH = "protein_length";
  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_GENE_PRODUCT = "gene_product";

  private final boolean _useShortDefline;
  private final int _startOffset;
  private final Anchor _startAnchor;
  private final int _endOffset;
  private final Anchor _endAnchor;

  public ProteinFeatureProvider(JSONObject config) {
    _useShortDefline = useShortDefline(config);

    _startOffset = config.getInt("startOffset3");
    _startAnchor = Anchor.valueOf(config.getString("startAnchor3"));

    _endOffset = config.getInt("endOffset3");
    _endAnchor = Anchor.valueOf(config.getString("endAnchor3"));
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
    String strand = ".";

    Integer segmentStart = getPositionProtein(featureLength, _startOffset, _startAnchor);
    Integer segmentEnd = getPositionProtein(featureLength, _endOffset, _endAnchor);

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline) {
      defline.append("  | ");
      defline.append(stringValue(record, ATTR_ORGANISM));
      defline.append(" | ");
      defline.append(stringValue(record, ATTR_GENE_PRODUCT));
      defline.append(" | protein | ");
      defline.append("" + segmentStart);
      defline.append(" to ");
      defline.append("" + segmentEnd);
      defline.append(" (");
      defline.append(getPositionDescProtein(_startOffset, "+", _startAnchor));
      defline.append(" to ");
      defline.append(getPositionDescProtein(_endOffset, "-", _endAnchor));
      defline.append(")");
      defline.append(" | segment_length=");
      defline.append(""+(segmentEnd - segmentStart + 1));
    }

    return List.of(List.of(chrom, segmentStart.toString(), segmentEnd.toString(), defline.toString(), ".", strand));
  }

  private static Integer getPositionProtein(Integer featureLength, int offset, Anchor anchor) throws WdkModelException {
    switch(anchor){
      case Start: return 1 + offset;
      case End: return featureLength - offset;
      default: throw new WdkModelException("Unsupported anchor type: " + anchor);
    }
  }

  private static String getPositionDescProtein(int offset, String sign, Anchor anchor){
    return offset == 0
        ? anchor.name()
        : anchor.name() + sign + offset;
  }
}
