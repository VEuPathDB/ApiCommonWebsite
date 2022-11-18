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
import org.json.JSONObject;

public class TranscriptBlockFeaturesProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";
  private static final String TABLE_GENE_MODEL_DUMP = "GeneModelDump";

  private final boolean _useShortDefline;
  private final Set<String> _allowedFeatureTypes;
  private final String _blockFeatureType;

  public TranscriptBlockFeaturesProvider(JSONObject config, Set<String> allowedFeatureTypes, String blockFeatureType) {
    _useShortDefline = useShortDefline(config);
    _allowedFeatureTypes = allowedFeatureTypes;
    _blockFeatureType = blockFeatureType;
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.GENE_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM } ;
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
      Map<String, Set<String>> strandsByT = new HashMap<>();
      Map<String, List<Integer>> startsByT = new HashMap<>();
      Map<String, List<String>> startStringsByT = new HashMap<>();
      Map<String, List<Integer>> endsByT = new HashMap<>();
      Map<String, List<Integer>> lengthsByT = new HashMap<>();
      Map<String, List<String>> lengthStringsByT = new HashMap<>();

      TableValue geneModelDumpRows = record.getTableValue(TABLE_GENE_MODEL_DUMP);
      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        if(!_allowedFeatureTypes.contains(geneModelDumpRow.get("type").toString())){
          continue;
        }
        //String sourceId = geneModelDumpRow.get("source_id").toString();
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

        StringBuilder defline = new StringBuilder(transcriptId);
        if (!_useShortDefline) {
          defline.append("  | ");
          defline.append(stringValue(record, ATTR_ORGANISM));
          defline.append(" | ");
          defline.append(_blockFeatureType);
          defline.append(" | segment_length=");
          // https://stackoverflow.com/a/17846520
          defline.append(lengthsByT.get(transcriptId).stream().mapToInt(Integer::intValue).sum());
        }

        Integer segmentStart = Collections.min(startsByT.get(transcriptId));
        Integer segmentEnd = Collections.max(endsByT.get(transcriptId));
        Integer blockCount = startsByT.get(transcriptId).size();
        String blockSizes = String.join(",", lengthStringsByT.get(transcriptId));
        String blockStarts = String.join(",", startStringsByT.get(transcriptId));
        result.add(List.of(chrom, "" + segmentStart, "" + segmentEnd, defline.toString(), ".", strand, ".", ".", ".", ""+blockCount, blockSizes, blockStarts));
      }
      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }

}
