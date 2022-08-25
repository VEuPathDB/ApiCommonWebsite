package org.gusdb.wdk.model.report.reporter.bed;

import org.gusdb.wdk.model.report.reporter.bed.BedReporter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public class BedDynSpanReporter extends BedReporter {

  protected List<List<String>> recordAsBedFields(JSONObject config, RecordInstance record){
    return Arrays.asList(recordAsBedFieldsDynSpan(config, record));
  }


  //Pf3D7_03_v3:1-100:f
  private static Pattern dynspanSourceIdPattern = Pattern.compile("^(.*):(\\d+)-(\\d+):(f|r)$");

  private static String stringValue(RecordInstance record, String key){
    try {
      return record.getAttributeValue(key).toString();
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }

  private static Matcher matchLocationCoords(RecordInstance record, String key, Pattern p){
    String text = stringValue(record, key);
    Matcher m = p.matcher(text);
    if (!m.matches()){
      throw new WdkRuntimeException(String.format("attribute %s with value %s not matching pattern %s", key, text, p.toString()));
    }
    return m;
  }
  
  private static String bedStrand(String idStrand){
    switch(idStrand){
      case "f":
        return "+";
      case "r":
        return "-";
      default:
        return idStrand;
    }
  }
  private static String longStrand(String shortSign){
    switch(shortSign){
      case "+":
        return "forward";
      case "-":
        return "reverse";
      default:
        return shortSign;
    }
  }
  private static String getSourceId(RecordInstance record){
    return record.getPrimaryKey().getValues().get("source_id");
  }

  private static List<String> recordAsBedFieldsDynSpan(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    Matcher m = dynspanSourceIdPattern.matcher(featureId);
    if (!m.matches()){
      throw new WdkRuntimeException(String.format("Genomic segment ID %s not matching pattern %s", featureId, dynspanSourceIdPattern.toString()));
    }

    String chrom = m.group(1);
    Integer segmentStart = Integer.valueOf(m.group(2));
    Integer segmentEnd = Integer.valueOf(m.group(3));
    String strand = bedStrand(m.group(4));

    String defline;
    StringBuilder sb = new StringBuilder(featureId);
    if("short".equals(config.getString("deflineType"))){
      defline = sb.toString();
    } else {
      sb.append("  | ");
      sb.append(stringValue(record, "organism"));
      sb.append(" | segment of genomic sequence | ");
      sb.append(chrom);
      sb.append(", ");
      sb.append(longStrand(strand) + " strand");
      sb.append(", ");
      sb.append(""+segmentStart);
      sb.append(" to ");
      sb.append(""+segmentEnd);
      sb.append(" | segment_length=");
      sb.append(""+(segmentEnd - segmentStart + 1));
      defline = sb.toString();
    }
    return Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand);
  }

}
