package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;

public class GeneComponentsFeatureProvider implements BedFeatureProvider {

  private enum GeneComponentAsRequested {
    five_prime_utr,
    three_prime_utr,
    exon,
    intron;
  }

  private enum GeneComponentAsStored {
    UTR,
    CDS,
    Exon,
    Intron;
  }

  private static final String ATTR_ORGANISM = "organism";
  private static final String TABLE_GENE_MODEL_DUMP = "GeneModelDump";

  private final boolean _useShortDefline;
  private final Set<GeneComponentAsRequested> _requestedComponents;

  public GeneComponentsFeatureProvider(JSONObject config){
    _useShortDefline = useShortDefline(config);
    _requestedComponents = config.getJSONArray("geneComponents").toList().stream().map(o -> GeneComponentAsRequested.valueOf(o.toString())).collect(Collectors.toSet());

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
      Map<String, List<Integer>> startsByT = new HashMap<>();
      Map<String, List<Integer>> endsByT = new HashMap<>();
      TableValue geneModelDumpRows = record.getTableValue(TABLE_GENE_MODEL_DUMP);

      /*
       * Iterate twice. First get starts and ends for each transcript - we need them to orient UTRs
       */
      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        Integer gmStart = Integer.valueOf(geneModelDumpRow.get("gm_start").toString());
        Integer gmEnd = Integer.valueOf(geneModelDumpRow.get("gm_end").toString());
        for(String transcriptId: geneModelDumpRow.get("transcript_ids").toString().split(",")){
          if(!startsByT.containsKey(transcriptId)){
            startsByT.put(transcriptId, new ArrayList<Integer>());
            endsByT.put(transcriptId, new ArrayList<Integer>());
          }
          startsByT.get(transcriptId).add(gmStart);
          endsByT.get(transcriptId).add(gmEnd);
        }
      }
      Map<String, Integer> tStartsByT = startsByT.entrySet().stream()
                                    .collect(Collectors.toMap(
                                      entry -> entry.getKey(),
                                      entry -> Collections.min(entry.getValue())));
      Map<String, Integer> tEndsByT = endsByT.entrySet().stream()
                                    .collect(Collectors.toMap(
                                      entry -> entry.getKey(),
                                      entry -> Collections.max(entry.getValue())));

      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        String sourceId = geneModelDumpRow.get("source_id").toString();
        String sequenceId = geneModelDumpRow.get("sequence_id").toString();
        GeneComponentAsStored geneComponentAsStored = GeneComponentAsStored.valueOf(geneModelDumpRow.get("type").toString());
        Integer start = Integer.valueOf(geneModelDumpRow.get("gm_start").toString());
        Integer end = Integer.valueOf(geneModelDumpRow.get("gm_end").toString());
        Integer length = end - start;
        StrandDirection strand = StrandDirection.fromSign(geneModelDumpRow.get("strand").toString());

        boolean startMatchesTranscriptStart = Arrays.asList(geneModelDumpRow.get("transcript_ids").toString().split(","))
          .stream()
          .map(transcriptId -> tStartsByT.get(transcriptId).equals(start))
          .reduce(Boolean.FALSE, Boolean::logicalOr);

        boolean endMatchesTranscriptEnd = Arrays.asList(geneModelDumpRow.get("transcript_ids").toString().split(","))
          .stream()
          .map(transcriptId -> tEndsByT.get(transcriptId).equals(end))
          .reduce(Boolean.FALSE, Boolean::logicalOr);

        GeneComponentAsRequested geneComponent = asRequestedComponent(geneComponentAsStored, strand, startMatchesTranscriptStart, endMatchesTranscriptEnd);
        if(_requestedComponents.contains(geneComponent)){
          StringBuilder defline = new StringBuilder(sourceId + "::" + geneComponent.toString());
          if (!_useShortDefline) {
            defline.append("  | ");
            defline.append(stringValue(record, ATTR_ORGANISM));
            defline.append(" | ");
            defline.append(stringValue(record, "gene_product"));
            defline.append(" | segment_length=");
            defline.append("" + (end-start));
          }
        result.add(List.of(sequenceId, "" + start, "" + end, defline.toString(), ".", strand.getSign()));
        }
      }
      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }
  private GeneComponentAsRequested asRequestedComponent(GeneComponentAsStored geneComponentAsStored, StrandDirection strand, boolean startMatchesTranscriptStart, boolean endMatchesTranscriptEnd){
    switch(geneComponentAsStored){
      case Exon:
      case CDS:
        return GeneComponentAsRequested.exon;
      case Intron:
        return GeneComponentAsRequested.intron;
      case UTR:
        switch (strand){
          case forward:
            return startMatchesTranscriptStart ? GeneComponentAsRequested.five_prime_utr : GeneComponentAsRequested.three_prime_utr;
          case reverse:
            return endMatchesTranscriptEnd ? GeneComponentAsRequested.five_prime_utr: GeneComponentAsRequested.three_prime_utr;
        }
    }
    return null;
  }
}


