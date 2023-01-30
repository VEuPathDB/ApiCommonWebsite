package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.json.JSONObject;

public class GeneGenomicFeatureProvider implements BedFeatureProvider {

  private enum OffsetSign {
    plus,
    minus;
  }

  private enum Anchor {
    Start,
    End,
    CodeStart,
    CodeEnd
  }

  // "PbANKA_01_v3:438265..440094(-)"
  private static final Pattern LOCATION_TEXT_PATTERN = Pattern.compile("^(.*):(\\d+)..(\\d+)\\((\\+|-)\\)$");

  private static final String ATTR_LOCATION_TEXT = "location_text";
  private static final String ATTR_THREE_PRIME_UTR_LENGTH = "three_prime_utr_length";
  private static final String ATTR_FIVE_PRIME_UTR_LENGTH = "five_prime_utr_length";
  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_GENE_NAME = "gene_name";
  private static final String ATTR_GENE_PRODUCT = "gene_product";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final boolean _reverseAndComplement;
  private final int _upstreamOffset;
  private final OffsetSign _upstreamSign;
  private final Anchor _upstreamAnchor;
  private final int _downstreamOffset;
  private final OffsetSign _downstreamSign;
  private final Anchor _downstreamAnchor;

  public GeneGenomicFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
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
    return new String[] { ATTR_LOCATION_TEXT , ATTR_ORGANISM, ATTR_THREE_PRIME_UTR_LENGTH, ATTR_FIVE_PRIME_UTR_LENGTH, ATTR_GENE_NAME, ATTR_GENE_PRODUCT};
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
    int fivePrimeUtrLength = integerValueWithZeroForEmpty(record, ATTR_FIVE_PRIME_UTR_LENGTH);
    int threePrimeUtrLength = integerValueWithZeroForEmpty(record, ATTR_THREE_PRIME_UTR_LENGTH);

    Integer segmentStart = getPositionGenomic(featureStart, featureEnd, _upstreamOffset, _upstreamSign, _upstreamAnchor, fivePrimeUtrLength, threePrimeUtrLength);
    Integer segmentEnd = getPositionGenomic(featureStart, featureEnd, _downstreamOffset, _downstreamSign, _downstreamAnchor, fivePrimeUtrLength, threePrimeUtrLength);

    DeflineBuilder defline = new DeflineBuilder(featureId);

    if(_requestedDeflineFields.contains("organism")){
      defline.appendRecordAttribute(record, ATTR_ORGANISM);
    }
    if(_requestedDeflineFields.contains("description")){
      defline.appendTwoRecordAttributesWhereFirstOneMayBeEmpty(record, ATTR_GENE_NAME, ATTR_GENE_PRODUCT);
    }
    if(_requestedDeflineFields.contains("position")){
      defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
    }
    if(_requestedDeflineFields.contains("ui_choice")){
      defline.appendRangeUiChoice(
        "Unspliced Genomic Sequence",
        getPositionDescGenomic(_upstreamOffset, _upstreamSign, _upstreamAnchor),
        getPositionDescGenomic(_downstreamOffset, _downstreamSign, _downstreamAnchor),
        _reverseAndComplement
      );
    }
    if(_requestedDeflineFields.contains("segment_length")){
      defline.appendSegmentLength(segmentStart, segmentEnd);
    }

    return List.of(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
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
      int offset, OffsetSign sign, Anchor anchor,
      int fivePrimeUtrLength, int threePrimeUtrLength
      ) throws WdkModelException{
    if (sign == OffsetSign.minus) {
      offset = - offset;
    }
    switch(anchor) {
      case Start: return featureStart + offset;
      case CodeStart: return featureStart + fivePrimeUtrLength + offset;
      case End: return featureEnd + offset;
      case CodeEnd: return featureEnd - threePrimeUtrLength + offset;
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
