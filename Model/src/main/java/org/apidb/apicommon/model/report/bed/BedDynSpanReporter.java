package org.apidb.apicommon.model.report.bed;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class BedDynSpanReporter extends BedReporter implements BedFeatureProvider {

  //Pf3D7_03_v3:1-100:f
  private static final Pattern DYN_SPAN_SOURCE_ID_PATTERN = Pattern.compile("^(.*):(\\d+)-(\\d+):(f|r)$");

  private static final String ATTR_ORGANISM = "organism";

  private boolean _useShortDefline;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    _useShortDefline = useShortDefline(config);
    return configure(this);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "DynSpanRecordClasses.DynSpanRecordClass";
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[0];
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    String featureId = getSourceId(record);
    Matcher m = DYN_SPAN_SOURCE_ID_PATTERN.matcher(featureId);
    if (!m.matches()){
      throw new WdkModelException(String.format("Genomic segment ID %s not matching pattern %s", featureId, DYN_SPAN_SOURCE_ID_PATTERN.toString()));
    }

    String chrom = m.group(1);
    Integer segmentStart = Integer.valueOf(m.group(2));
    Integer segmentEnd = Integer.valueOf(m.group(3));
    String strand = bedStrand(m.group(4));

    StringBuilder defline = new StringBuilder(featureId);
    if (!_useShortDefline){
      defline.append("  | ");
      defline.append(stringValue(record, ATTR_ORGANISM));
      defline.append(" | segment of genomic sequence | ");
      defline.append(chrom);
      defline.append(", ");
      defline.append(longStrand(strand) + " strand");
      defline.append(", ");
      defline.append(""+segmentStart);
      defline.append(" to ");
      defline.append(""+segmentEnd);
      defline.append(" | segment_length=");
      defline.append(""+(segmentEnd - segmentStart + 1));
    }

    return List.of(List.of(chrom, "" + segmentStart, "" + segmentEnd, defline.toString(), ".", strand));
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
}
