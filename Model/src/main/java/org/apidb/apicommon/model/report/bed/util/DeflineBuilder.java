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

  public DeflineBuilder appendPosition(String chrom, Integer segmentStart, Integer segmentEnd, StrandDirection strand){
    StringBuilder innerBuilder = new StringBuilder();
    innerBuilder.append(chrom);
    innerBuilder.append(", ");
    if(!strand.equals(StrandDirection.none)){
      innerBuilder.append(strand + " strand");
    }
    innerBuilder.append(", ");
    innerBuilder.append("" + segmentStart);
    innerBuilder.append(" to ");
    innerBuilder.append("" + segmentEnd);
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendSegmentLength(Integer segmentStart, Integer segmentEnd){
    StringBuilder innerBuilder = new StringBuilder("segment_length=");
    innerBuilder.append(""+(segmentEnd - segmentStart + 1));
    return appendValue(innerBuilder.toString());
  }

  public DeflineBuilder appendTotalSegmentLength(List<Integer> lengths){
    StringBuilder innerBuilder = new StringBuilder("segment_length=");
    innerBuilder.append("" + lengths.stream().mapToInt(Integer::intValue).sum());
    return appendValue(innerBuilder.toString());
  }
}
