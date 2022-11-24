package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.json.JSONObject;

public class GeneGenomicFeatureProvider implements BedFeatureProvider {

  // "PbANKA_01_v3:438265..440094(-)"
  private static final Pattern LOCATION_TEXT_PATTERN = Pattern.compile("^(.*):(\\d+)..(\\d+)\\((\\+|-)\\)$");

  private static final String ATTR_LOCATION_TEXT = "location_text";

  private final boolean _useShortDefline;
  private final boolean _reverseAndComplement;
  private final int _upstreamOffset;
  private final OffsetSign _upstreamSign;
  private final Anchor _upstreamAnchor;
  private final int _downstreamOffset;
  private final OffsetSign _downstreamSign;
  private final Anchor _downstreamAnchor;

  public GeneGenomicFeatureProvider(JSONObject config) {
    _useShortDefline = useShortDefline(config);
    _reverseAndComplement = config.getBoolean("reverseAndComplement");

    _upstreamOffset = config.getInt("upstreamOffset");
    _upstreamSign = OffsetSign.valueOf(config.getString("upstreamSign"));
    _upstreamAnchor = Anchor.valueOf(config.getString("upstreamAnchor"));

    _downstreamOffset = config.getInt("downstreamOffset");
    _downstreamSign = OffsetSign.valueOf(config.getString("downstreamSign"));
    _downstreamAnchor = Anchor.valueOf(config.getString("downstreamAnchor"));
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.TRANSCRIPT_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_LOCATION_TEXT };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {

    String featureId = getSourceId(record);

    Matcher m = matchLocationCoords(record, ATTR_LOCATION_TEXT, LOCATION_TEXT_PATTERN);
    String chrom = m.group(1);
    int featureStart = Integer.valueOf(m.group(2));
    int featureEnd = Integer.valueOf(m.group(3));

    StrandDirection strand = StrandDirection.fromSign(m.group(4));
    if(_reverseAndComplement) {
      strand = strand.opposite();
    }

    Integer segmentStart = getPositionGenomic(featureStart, featureEnd, _upstreamOffset, _upstreamSign, _upstreamAnchor);
    Integer segmentEnd = getPositionGenomic(featureStart, featureEnd, _downstreamOffset, _downstreamSign, _downstreamAnchor);

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline){
      defline.append("  | ");
      defline.append(stringValue(record, "organism"));
      defline.append(" | ");
      defline.append(stringValue(record, "gene_product"));
      defline.append(" | locus sequence | ");
      defline.append(chrom);
      defline.append(", ");
      defline.append(strand + " strand");
      defline.append(", ");
      defline.append("" + segmentStart);
      defline.append(" to ");
      defline.append("" + segmentEnd);
      defline.append(" (");
      defline.append(getPositionDescGenomic(_upstreamOffset, _upstreamSign, _upstreamAnchor));
      defline.append(" to ");
      defline.append(getPositionDescGenomic(_downstreamOffset, _downstreamSign, _downstreamAnchor));
      defline.append(") | segment_length=");
      defline.append(""+(segmentEnd - segmentStart + 1));
    }

    return List.of(List.of(chrom, segmentStart.toString(), segmentEnd.toString(), defline.toString(), ".", strand.getSign()));
  }

  private Matcher matchLocationCoords(RecordInstance record, String key, Pattern p) throws WdkModelException{
    String text = stringValue(record, key);
    Matcher m = p.matcher(text);
    if (!m.matches()){
      throw new WdkModelException(String.format("attribute %s with value %s not matching pattern %s", key, text, p.toString()));
    }
    return m;
  }

  private static int getPositionGenomic(
      Integer featureStart, Integer featureEnd, 
      int offset, OffsetSign sign, Anchor anchor) throws WdkModelException{
    if (sign == OffsetSign.minus) {
      offset = - offset;
    }
    switch(anchor) {
      case Start: return featureStart + offset;
      case End: return featureEnd + offset;
      default: throw new WdkModelException("Unsupported anchor type: " + anchor);
    }
  }

  private static String getPositionDescGenomic(int offset, OffsetSign sign, Anchor anchor) throws WdkModelException{
    return offset == 0
        ? anchor.name()
        : anchor + offsetSignShort(sign) + offset;
  }

  private static String offsetSignShort(OffsetSign sign) throws WdkModelException{
    switch(sign){
      case plus:
        return "+";
      case minus:
        return "-";
      default:
        throw new WdkModelException(String.format("Unknown offset sign: %s", sign));
    }
  }

}
