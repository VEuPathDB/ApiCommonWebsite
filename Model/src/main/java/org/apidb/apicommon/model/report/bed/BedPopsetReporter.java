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

public class BedPopsetReporter extends BedReporter {

  protected List<List<String>> recordAsBedFields(JSONObject config, RecordInstance record){
    return Arrays.asList(recordAsBedFieldsPopset(config, record));
  }

  private static String stringValue(RecordInstance record, String key){
    try {
      return record.getAttributeValue(key).toString();
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }
  
  private static String getSourceId(RecordInstance record){
    return record.getPrimaryKey().getValues().get("source_id");
  }

  private static List<String> recordAsBedFieldsPopset(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    String chrom = featureId;
    String strand = ".";

    Integer featureLength = Integer.valueOf(stringValue(record, "sequence_length").replaceAll(",", ""));
    Integer segmentStart = 1;
    Integer segmentEnd = featureLength;

    String defline;
    StringBuilder sb = new StringBuilder(featureId);
    if("short".equals(config.getString("deflineType"))){
      defline = sb.toString();
    } else {
      sb.append("  | ");
      sb.append(stringValue(record, "organism"));
      sb.append(" | popset | ");
      sb.append(stringValue(record, "description"));
      sb.append(" | segment_length=");
      sb.append(""+featureLength);
      defline = sb.toString();
    }
    return Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand);
  }

}
