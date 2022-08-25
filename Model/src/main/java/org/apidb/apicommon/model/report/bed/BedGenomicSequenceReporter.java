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

public class BedGenomicSequenceReporter extends BedReporter {

  protected List<List<String>> recordAsBedFields(JSONObject config, RecordInstance record){
    String sequenceFeature = config.getString("sequenceFeature");
    switch (sequenceFeature){
      case "whole_sequence":
        return Arrays.asList(recordAsBedFieldsGenomicSequence(config, record));
      case "low_complexity":
        return featuresAsListOfBedFieldsTableQuery(config, record, "LowComplexity", "start_min", "end_max");
      case "repeats":
        return featuresAsListOfBedFieldsTableQuery(config, record, "Repeats", "start_min", "end_max");
      case "tandem":
        return featuresAsListOfBedFieldsTableQuery(config, record, "TandemRepeats", "start_min", "end_max");
      case "centromere":
        return featuresAsListOfBedFieldsTableQuery(config, record, "Centromere", "start_min", "end_max");
      default:
        throw new WdkRuntimeException(String.format("Unknown sequence feature: %s", sequenceFeature));
    }
  }

  private static List<List<String>> featuresAsListOfBedFieldsTableQuery(JSONObject config, RecordInstance record, String tableQueryName, String startName, String endName){
    List<List<String>> result = new ArrayList<>();
    try {
      String featureId = getSourceId(record);
      String chrom = featureId;
      String longStrand = config.getString("strand");
      String strand = strandSign(longStrand);
      String organism;
      if("short".equals(config.getString("deflineType"))){
        organism = "unused";
      } else {
        organism = stringValue(record, "organism");
      }
      TableValue rows = record.getTableValue(tableQueryName);
      for (Map<String, AttributeValue> row : rows) {
        Integer start = Integer.valueOf(row.get(startName).toString());
        Integer end = Integer.valueOf(row.get(endName).toString());
        String defline;
        StringBuilder sb = new StringBuilder(chrom + "::" + start + "-" + end); 
        if("short".equals(config.getString("deflineType"))){
          defline = sb.toString();
        } else {
          sb.append("  | ");
          sb.append(organism);
          sb.append(" | ");
          sb.append(tableQueryName); 
          sb.append(" | ");
          sb.append(chrom);
          sb.append(", ");
          sb.append(""+start);
          sb.append(" to ");
          sb.append(""+end);
          sb.append(" | ");
          sb.append("sequence of ");
          sb.append(longStrand + " strand");
          sb.append(" | segment_length=");
          sb.append(""+(end - start + 1));
          defline = sb.toString();
        }
        result.add(Arrays.asList(chrom, ""+ start,""+ end, defline, ".", strand));
        }
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }

    return result;
  }

  private static String stringValue(RecordInstance record, String key){
    try {
      return record.getAttributeValue(key).toString();
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }

  private static String strandSign(String longStrand){
    switch(longStrand){
      case "forward":
        return "+";
      case "reverse":
        return "-";
      default:
        throw new WdkRuntimeException(String.format("Unknown strand option: %s", longStrand));
    }
  }
  
  private static String getSourceId(RecordInstance record){
    return record.getPrimaryKey().getValues().get("source_id");
  }

  private static List<String> recordAsBedFieldsGenomicSequence(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    String chrom = featureId;
    String longStrand = config.getString("strand");
    String strand = strandSign(longStrand);
    Integer featureLength = Integer.valueOf(stringValue(record, "formatted_length").replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;
    String defline;
    StringBuilder sb = new StringBuilder(featureId);
    if("short".equals(config.getString("deflineType"))){
      defline = sb.toString();
    } else {
      sb.append("  | ");
      sb.append(stringValue(record, "organism"));
      sb.append(" | genomic sequence | ");
      sb.append(chrom);
      sb.append(", ");
      sb.append(longStrand + " strand");
      sb.append(" | segment_length=");
      sb.append(""+featureLength);
      defline = sb.toString();
    }
    return Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand);
  }

}
