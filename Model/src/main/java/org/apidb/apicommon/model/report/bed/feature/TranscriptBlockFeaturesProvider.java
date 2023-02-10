package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.json.JSONObject;

public class TranscriptBlockFeaturesProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";
  private static final String ATTR_NAME = "name";
  private static final String ATTR_PRODUCT = "product";
  private static final String TABLE_GENE_MODEL_DUMP = "GeneModelDump";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final String _geneModelDumpType;
  private final String _blockFeatureType;

  public TranscriptBlockFeaturesProvider(JSONObject config, String geneModelDumpType, String blockFeatureType) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _geneModelDumpType = geneModelDumpType;
    _blockFeatureType = blockFeatureType;
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.GENE_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
      ATTR_ORGANISM,
      ATTR_NAME,
      ATTR_PRODUCT } ;
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[] { TABLE_GENE_MODEL_DUMP };
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    try {
      List<List<String>> result = new ArrayList<>();

      Map<String, Set<String>> sequencesByT = new HashMap<>();
      Map<String, Set<StrandDirection>> strandsByT = new HashMap<>();
      Map<String, List<Integer>> startsByT = new HashMap<>();
      Map<String, List<Integer>> endsByT = new HashMap<>();
      Map<String, List<Integer>> lengthsByT = new HashMap<>();

      TableValue geneModelDumpRows = record.getTableValue(TABLE_GENE_MODEL_DUMP);
      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        if(!_geneModelDumpType.equals(geneModelDumpRow.get("type").toString())){
          continue;
        }
        //String sourceId = geneModelDumpRow.get("source_id").toString();
        String sequenceId = geneModelDumpRow.get("sequence_id").toString();
        Integer gmStart = Integer.valueOf(geneModelDumpRow.get("gm_start").toString());
        Integer gmEnd = Integer.valueOf(geneModelDumpRow.get("gm_end").toString());
        Integer length = gmEnd - gmStart;
        StrandDirection strand = StrandDirection.fromSign(geneModelDumpRow.get("strand").toString());
        for(String transcriptId: geneModelDumpRow.get("transcript_ids").toString().split(",")){
          if(!sequencesByT.containsKey(transcriptId)){
            sequencesByT.put(transcriptId, new HashSet<String>());
            strandsByT.put(transcriptId, new HashSet<StrandDirection>());
            startsByT.put(transcriptId, new ArrayList<Integer>());
            endsByT.put(transcriptId, new ArrayList<Integer>());
            lengthsByT.put(transcriptId, new ArrayList<Integer>());
          }
          sequencesByT.get(transcriptId).add(sequenceId);
          strandsByT.get(transcriptId).add(strand);
          startsByT.get(transcriptId).add(gmStart);
          endsByT.get(transcriptId).add(gmEnd);
          lengthsByT.get(transcriptId).add(length);
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
        StrandDirection strand = strandsByT.get(transcriptId).iterator().next();
        Integer segmentStart = Collections.min(startsByT.get(transcriptId));
        Integer segmentEnd = Collections.max(endsByT.get(transcriptId));

        DeflineBuilder defline = new DeflineBuilder(_blockFeatureType.toLowerCase() + "_" + transcriptId);

        if(_requestedDeflineFields.contains("organism")){
          defline.appendRecordAttribute(record, ATTR_ORGANISM);
        }
        if(_requestedDeflineFields.contains("description")){
          defline.appendTwoRecordAttributesWhereFirstOneMayBeEmpty(record, ATTR_NAME, ATTR_PRODUCT);
        }
        if(_requestedDeflineFields.contains("position")){
          defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
        }
        if(_requestedDeflineFields.contains("ui_choice")){
          defline.appendValue(_blockFeatureType);
        }
        if(_requestedDeflineFields.contains("segment_length")){
          defline.appendTotalSplicedLength(lengthsByT.get(transcriptId));
        }

        result.add(BedLine.bed12(chrom, defline, strand, startsByT.get(transcriptId), endsByT.get(transcriptId)));
      }
      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }

}
