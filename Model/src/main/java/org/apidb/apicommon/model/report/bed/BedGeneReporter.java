package org.gusdb.wdk.model.report.reporter.bed;

import org.gusdb.wdk.model.report.reporter.bed.BedReporter;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

import org.apidb.apicommon.model.TranscriptUtil;

public class BedGeneReporter extends BedReporter {

  private String _originalQuestionName;

  @Override
  public BedGeneReporter configure(JSONObject config) throws ReporterConfigException {
    try {
      _originalQuestionName = _baseAnswer.getAnswerSpec().getQuestion().getName();
      if(configNeedsGeneAnswer(config)){
        _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer);
      }
      super.configure(config);
      return this;
    }
    catch (WdkUserException e) {
      throw new ReporterConfigException(e.getMessage());
    }
    catch (WdkModelException e) {
      throw new WdkRuntimeException("Could not create in-memory step from incoming answer spec", e);
    }
  }

  @Override
  public String getDownloadFileName() {
    return _originalQuestionName + ".tsv";
  }

  private static boolean configNeedsGeneAnswer(JSONObject config){
    String type = config.getString("type");
    switch(type){
      case "genomic":
      case "protein":
        return false;
      case "protein_features":
      case "genomic_features":
      case "cds":
      case "transcript":
        return true;
      default:
        throw new WdkRuntimeException(String.format("Unknown sequence type: %s", type));
    }
  }

  protected List<List<String>> recordAsBedFields(JSONObject config, RecordInstance record){
    String type = config.getString("type");
    switch(type){
      case "genomic":
        return Arrays.asList(recordAsBedFieldsGenomic(config, record));
      case "protein":
        return Arrays.asList(recordAsBedFieldsProtein(config, record));
      case "protein_features":
        String proteinFeature = config.getString("proteinFeature");
        switch (proteinFeature){
          case "interpro":
            return featuresAsListOfBedFieldsProteinInterpro(config, record);
          case "signalp":
            return featuresAsListOfBedFieldsTableQuery(config, record, "SignalP", "spf_start_min", "spf_end_max");
          case "tmhmm":
            return featuresAsListOfBedFieldsTableQuery(config, record, "TMHMM", "tmf_start_min", "tmf_end_max");
          case "low_complexity":
            return featuresAsListOfBedFieldsTableQuery(config, record, "LowComplexity", "lc_start_min", "lc_end_max");
          default:
            throw new WdkRuntimeException(String.format("Unknown protein feature: %s", proteinFeature));
        }
      case "cds":
        return recordAsListOfBlockFeaturesByTranscript(config, record, Set.of("CDS"), "cds");
      case "transcript":
        return recordAsListOfBlockFeaturesByTranscript(config, record, Set.of("CDS", "UTR"), "transcript");
      default:
        throw new WdkRuntimeException(String.format("Unknown sequence type: %s", type));
    }
  }


  // "PbANKA_01_v3:438265..440094(-)"
  private static Pattern locationTextPattern = Pattern.compile("^(.*):(\\d+)..(\\d+)\\((\\+|-)\\)$");

  private static String stringValue(RecordInstance record, String key){
    try {
      return record.getAttributeValue(key).toString();
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }
  }

  private static String strandOptionAsStrandSign(String sign){
    switch(sign){
      case "plus":
        return "+";
      case "minus":
        return "-";
      default:
        throw new WdkRuntimeException(String.format("Unknown strand option: %s", sign));
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

  private static String oppositeStrand(String shortSign){
    switch(shortSign){
      case "+":
        return "-";
      case "-":
        return "+";
      default:
        return shortSign;
    }
  }

  private static Integer getPositionGenomic(JSONObject config, Integer featureStart, Integer featureEnd, 
      String offsetKey, String signKey, String anchorKey){
    String sign = config.getString(signKey);
    Integer offset;
    switch(sign){
      case "plus":
        offset = config.getInt(offsetKey);
        break;
      case "minus":
        offset = - config.getInt(offsetKey);
        break;
      default:
        throw new WdkRuntimeException(String.format("%s value should be 'plus' or 'minus', got: %s", signKey, sign));
    }

    Integer result;
    String anchor = config.getString(anchorKey);
    switch(anchor){
      case "Start":
        result = featureStart + offset;
        break;
      case "End":
        result = featureEnd + offset;
        break;
      default:
        throw new WdkRuntimeException(String.format("%s value should be 'Start' or 'End', got: %s", anchorKey, anchor));
    }
    return result;
  }

  private static Integer getPositionProtein(JSONObject config, Integer featureLength, 
      String offsetKey, String anchorKey){
    Integer offset = config.getInt(offsetKey);

    Integer result;
    String anchor = config.getString(anchorKey);
    switch(anchor){
      case "Start":
        result = 1 + offset;
        break;
      case "End":
        result = featureLength - offset;
        break;
      default:
        throw new WdkRuntimeException(String.format("%s value should be 'Start' or 'End', got: %s", anchorKey, anchor));
    }
    return result;
  }

  private static String getPositionDescGenomic(JSONObject config,
      String offsetKey, String signKey, String anchorKey){
    Integer offset = Integer.valueOf(config.getInt(offsetKey));
    if(offset == 0){
        return config.getString(anchorKey);
    } else {
        return config.getString(anchorKey) + strandOptionAsStrandSign(config.getString(signKey)) + offset.toString();
    }
  }
  private static String getPositionDescProtein(JSONObject config,
      String offsetKey, String sign, String anchorKey){
    Integer offset = Integer.valueOf(config.getInt(offsetKey));
    if(offset == 0){
        return config.getString(anchorKey);
    } else {
        return config.getString(anchorKey) + sign + offset.toString();
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
  
  private static String getSourceId(RecordInstance record){
    return record.getPrimaryKey().getValues().get("source_id");
  }

  private static List<String> recordAsBedFieldsGenomic(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    Matcher m = matchLocationCoords(record, "location_text", locationTextPattern);
    String chrom = m.group(1);
    Integer featureStart = Integer.valueOf(m.group(2));
    Integer featureEnd = Integer.valueOf(m.group(3));
    String strand = m.group(4);
    if(config.getBoolean("reverseAndComplement")){
      strand = oppositeStrand(strand);
    }
    Integer segmentStart = getPositionGenomic(config, featureStart, featureEnd, "upstreamOffset", "upstreamSign", "upstreamAnchor");
    Integer segmentEnd = getPositionGenomic(config, featureStart, featureEnd, "downstreamOffset", "downstreamSign", "downstreamAnchor");
    
    String defline;
    StringBuilder sb = new StringBuilder(featureId);
    if("short".equals(config.getString("deflineType"))){
      defline = sb.toString();
    } else {
      sb.append("  | ");
      sb.append(stringValue(record, "organism"));
      sb.append(" | ");
      sb.append(stringValue(record, "gene_product"));
      sb.append(" | locus sequence | ");
      sb.append(chrom);
      sb.append(", ");
      sb.append(longStrand(strand) + " strand");
      sb.append(", ");
      sb.append("" + segmentStart);
      sb.append(" to ");
      sb.append("" + segmentEnd);
      sb.append(" (");
      sb.append(getPositionDescGenomic(config, "upstreamOffset", "upstreamSign", "upstreamAnchor"));
      sb.append(" to ");
      sb.append(getPositionDescGenomic(config, "downstreamOffset", "downstreamSign", "downstreamAnchor"));
      sb.append(") | segment_length=");
      sb.append(""+(segmentEnd - segmentStart + 1));
      defline = sb.toString();
    }
    return Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand);
  }

  private static List<String> recordAsBedFieldsProtein(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    String chrom = featureId;
    Integer featureLength = Integer.valueOf(stringValue(record, "protein_length"));
    String strand = ".";

    Integer segmentStart = getPositionProtein(config, featureLength, "startOffset3", "startAnchor3");
    Integer segmentEnd = getPositionProtein(config, featureLength, "endOffset3", "endAnchor3");
    
    String defline;
    StringBuilder sb = new StringBuilder(featureId);
    if("short".equals(config.getString("deflineType"))){
      // PBANKA_0111300
      defline = sb.toString();
    } else {
      sb.append("  | ");
      sb.append(stringValue(record, "organism"));
      sb.append(" | ");
      sb.append(stringValue(record, "gene_product"));
      sb.append(" | protein | ");
      sb.append("" + segmentStart);
      sb.append(" to ");
      sb.append("" + segmentEnd);
      sb.append(" (");
      sb.append(getPositionDescProtein(config, "startOffset3", "+", "startAnchor3"));
      sb.append(" to ");
      sb.append(getPositionDescProtein(config, "endOffset3", "-", "endAnchor3"));
      sb.append(")");
      sb.append(" | segment_length=");
      sb.append(""+(segmentEnd - segmentStart + 1));
      defline = sb.toString();
    }
    return Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand);
  }

  private static List<List<String>> recordAsListOfBlockFeaturesByTranscript(JSONObject config, RecordInstance record, Set<String> allowedFeatureTypes, String blockFeatureType){
    List<List<String>> result = new ArrayList<>();
    Map<String, Set<String>> sequencesByT = new HashMap<>();
    Map<String, Set<String>> strandsByT = new HashMap<>();
    Map<String, List<Integer>> startsByT = new HashMap<>();
    Map<String, List<String>> startStringsByT = new HashMap<>();
    Map<String, List<Integer>> endsByT = new HashMap<>();
    Map<String, List<Integer>> lengthsByT = new HashMap<>();
    Map<String, List<String>> lengthStringsByT = new HashMap<>();
    try {
      TableValue geneModelDumpRows = record.getTableValue("GeneModelDump");
      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        if(!allowedFeatureTypes.contains(geneModelDumpRow.get("type").toString())){
          continue;
        }
        String sourceId = geneModelDumpRow.get("source_id").toString();
        String sequenceId = geneModelDumpRow.get("sequence_id").toString();
        Integer gmStart = Integer.valueOf(geneModelDumpRow.get("gm_start").toString());
        Integer gmEnd = Integer.valueOf(geneModelDumpRow.get("gm_end").toString());
        Integer length = gmEnd - gmStart;
        String strand = geneModelDumpRow.get("strand").toString();
        for(String transcriptId: geneModelDumpRow.get("transcript_ids").toString().split(",")){
          if(!sequencesByT.containsKey(transcriptId)){
            sequencesByT.put(transcriptId, new HashSet<String>());
            strandsByT.put(transcriptId, new HashSet<String>());
            startsByT.put(transcriptId, new ArrayList<Integer>());
            startStringsByT.put(transcriptId, new ArrayList<String>());
            endsByT.put(transcriptId, new ArrayList<Integer>());
            lengthsByT.put(transcriptId, new ArrayList<Integer>());
            lengthStringsByT.put(transcriptId, new ArrayList<String>());
          }
          sequencesByT.get(transcriptId).add(sequenceId);
          strandsByT.get(transcriptId).add(strand);
          startsByT.get(transcriptId).add(gmStart);
          startStringsByT.get(transcriptId).add(gmStart.toString());
          endsByT.get(transcriptId).add(gmEnd);
          lengthsByT.get(transcriptId).add(length);
          lengthStringsByT.get(transcriptId).add(length.toString());
        }
      }
      for(String transcriptId: sequencesByT.keySet()){
        if(sequencesByT.get(transcriptId).size() > 1 ){
          throw new WdkRuntimeException(String.format("Non-unique sequence for transcript: %s", transcriptId));
        }
        if(strandsByT.get(transcriptId).size() > 1 ){
          throw new WdkRuntimeException(String.format("Non-unique strand for transcript: %s", transcriptId));
        }
        String chrom = sequencesByT.get(transcriptId).iterator().next();
        String strand = strandsByT.get(transcriptId).iterator().next();

        String defline;
        StringBuilder sb = new StringBuilder(transcriptId);
        if("short".equals(config.getString("deflineType"))){
          defline = sb.toString();
        } else {
          sb.append("  | ");
          sb.append(stringValue(record, "organism"));
          sb.append(" | ");
          sb.append(blockFeatureType);
          sb.append(" | segment_length=");
          // https://stackoverflow.com/a/17846520
          sb.append(lengthsByT.get(transcriptId).stream().mapToInt(Integer::intValue).sum());
          defline = sb.toString();
        }

        Integer segmentStart = Collections.min(startsByT.get(transcriptId));
        Integer segmentEnd = Collections.max(endsByT.get(transcriptId));
        Integer blockCount = startsByT.get(transcriptId).size();
        String blockSizes = String.join(",", lengthStringsByT.get(transcriptId));
        String blockStarts = String.join(",", startStringsByT.get(transcriptId));
        result.add(Arrays.asList(chrom, "" + segmentStart, "" + segmentEnd, defline, ".", strand, ".", ".", ".", ""+blockCount, blockSizes, blockStarts));
      }
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }

    return result;
  }

  private static List<List<String>> featuresAsListOfBedFieldsProteinInterpro(JSONObject config, RecordInstance record){
    String featureId = getSourceId(record);
    List<List<String>> result = new ArrayList<>();
    try {
      TableValue interproRows = record.getTableValue("InterPro");
      String organism;
      if("short".equals(config.getString("deflineType"))){
        organism = "unused";
      } else {
        organism = stringValue(record, "organism");
      }
      for (Map<String, AttributeValue> interproRow : interproRows) {
        Integer start = Integer.valueOf(interproRow.get("interpro_start_min").toString());
        Integer end = Integer.valueOf(interproRow.get("interpro_end_min").toString());
        /*
         * The start and end coordinates are on the protein,
         * but for identical positions, the rows get merged.
         * Hence 'transcript_ids' that we split.
         * Example: PF3D7_0108400
         */
        for(String transcriptId: interproRow.get("transcript_ids").toString().split(", ")){
          String chrom = transcriptId;
          String defline;
          StringBuilder sb = new StringBuilder(transcriptId + "::" + interproRow.get("interpro_primary_id").toString());
          if("short".equals(config.getString("deflineType"))){
            defline = sb.toString();
          } else {
            sb.append("  | ");
            sb.append(organism);
            sb.append(" | protein | ");
            sb.append(interproRow.get("interpro_name").toString());
            sb.append(" | ");
            sb.append(interproRow.get("interpro_desc").toString());
            sb.append(" | ");
            sb.append(chrom);
            sb.append(", ");
            sb.append(""+start);
            sb.append(" to ");
            sb.append(""+end);
            sb.append(" | segment_length=");
            sb.append(""+(end - start + 1));
            defline = sb.toString();
          }

          result.add(Arrays.asList(chrom, ""+start, ""+end, defline, ".", "."));
        }
      }
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }

    return result;
  }
  private static List<List<String>> featuresAsListOfBedFieldsTableQuery(JSONObject config, RecordInstance record, String tableQueryName, String startName, String endName){
    List<List<String>> result = new ArrayList<>();
    try {
      TableValue rows = record.getTableValue(tableQueryName);
      String organism;
      if("short".equals(config.getString("deflineType"))){
        organism = "unused";
      } else {
        organism = stringValue(record, "organism");
      }
      for (Map<String, AttributeValue> row : rows) {
        Integer start = Integer.valueOf(row.get(startName).toString());
        Integer end = Integer.valueOf(row.get(endName).toString());
        String transcriptId = row.get("transcript_id").toString();
        String chrom = transcriptId;
        String defline;
        StringBuilder sb = new StringBuilder(chrom + "::" + start + "-" + end);
        if("short".equals(config.getString("deflineType"))){
          defline = sb.toString();
        } else {
          sb.append("  | ");
          sb.append(organism);
          sb.append(" | protein | ");
          sb.append(tableQueryName);
          sb.append(" | ");
          sb.append(chrom);
          sb.append(", ");
          sb.append(""+start);
          sb.append(" to ");
          sb.append(""+end);
          sb.append(" | segment_length=");
          sb.append(""+(end - start + 1));
          defline = sb.toString();
        }
        result.add(Arrays.asList(chrom, ""+ start, ""+end, defline, ".", "."));
      }
    } catch (WdkModelException | WdkUserException e){
      throw new WdkRuntimeException(e);
    }

    return result;
  }

}
