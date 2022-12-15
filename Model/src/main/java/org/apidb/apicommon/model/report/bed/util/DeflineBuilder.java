package org.apidb.apicommon.model.report.bed.util;

import java.util.List;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public class DeflineBuilder {

  private final StringBuilder _stringBuilder;
  private boolean isFirstField;

  public DeflineBuilder(String featureId){
    _stringBuilder = new StringBuilder(featureId); 
    isFirstField = true;
  }

  @Override
  public String toString(){
    return _stringBuilder.toString();
  }

  public DeflineBuilder appendValue(String s){
    if(isFirstField){
      _stringBuilder.append("  | ");
      isFirstField = false;
    } else {
      _stringBuilder.append(" | ");
    }
    _stringBuilder.append(s);
    return this;
  }

  public DeflineBuilder appendAttributeValue(AttributeValue a){
    return appendValue(a.toString());
  }

  public DeflineBuilder appendRecordAttribute(RecordInstance record, String key){
    try {
      return appendValue(record.getAttributeValue(key).toString());
    }
    catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }

  public DeflineBuilder appendTwoRecordAttributesWhereFirstOneMayBeEmpty(RecordInstance record, String firstKey, String secondKey){
    String v1;
    String v2;
    try {
      v1 = record.getAttributeValue(firstKey).toString();
      v2 = record.getAttributeValue(secondKey).toString();
    }
    catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
    StringBuilder innerBuilder = new StringBuilder();
    if(!"".equals(v1)){
      innerBuilder.append(v1);
      innerBuilder.append(" ");
    }
    innerBuilder.append(v2);
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendPosition(String chrom, Integer segmentStart, Integer segmentEnd, StrandDirection strand){
    StringBuilder innerBuilder = new StringBuilder();
    innerBuilder.append(chrom);
    if(!strand.equals(StrandDirection.none)){
      innerBuilder.append(", ");
      innerBuilder.append(strand + " strand");
    }
    innerBuilder.append(", ");
    innerBuilder.append("" + segmentStart);
    innerBuilder.append(" to ");
    innerBuilder.append("" + segmentEnd);
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendPositionAa(String chrom, Integer segmentStart, Integer segmentEnd){
    StringBuilder innerBuilder = new StringBuilder(chrom);
    innerBuilder.append(", ");
    innerBuilder.append("" + segmentStart + " aa");
    innerBuilder.append(" to ");
    innerBuilder.append("" + segmentEnd + " aa");
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendSegmentLength(Integer segmentStart, Integer segmentEnd){
    StringBuilder innerBuilder = new StringBuilder("segment_length=");
    innerBuilder.append(""+(segmentEnd - segmentStart + 1));
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendTotalSplicedLength(List<Integer> lengths){
    StringBuilder innerBuilder = new StringBuilder("spliced_length=");
    innerBuilder.append("" + lengths.stream().mapToInt(Integer::intValue).sum());
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendRangeUiChoice(String option, String upstream, String downstream, boolean reverseAndComplement){
    StringBuilder innerBuilder = new StringBuilder(option);
    innerBuilder.append(", ");
    innerBuilder.append(upstream);
    innerBuilder.append(" to ");
    innerBuilder.append(downstream);
    if(reverseAndComplement){
      innerBuilder.append(", reversed and complemented");
    }
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendGenomicFeatureUiChoice(String feature, StrandDirection strand){
    StringBuilder innerBuilder = new StringBuilder(feature);
    innerBuilder.append(", sequence of ");
    innerBuilder.append(strand.name());
    innerBuilder.append(" strand");
    return appendValue(innerBuilder.toString());
  }
}
