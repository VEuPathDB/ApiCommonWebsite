package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public class ProteinInterproFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";
  private static final String TABLE_INTERPRO = "InterPro";

  private final boolean _useShortDefline;

  public ProteinInterproFeatureProvider(JSONObject config) {
    _useShortDefline = useShortDefline(config);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.GENE_RECORDCLASS;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] {
        ATTR_ORGANISM
    };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[] {
        TABLE_INTERPRO
    };
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    try {
      List<List<String>> result = new ArrayList<>();
      TableValue interproRows = record.getTableValue(TABLE_INTERPRO);
      String organism = _useShortDefline ? "unused" : stringValue(record, ATTR_ORGANISM);
      for (Map<String, AttributeValue> interproRow : interproRows) {
        Integer start = Integer.valueOf(interproRow.get("interpro_start_min").toString());
        Integer end = Integer.valueOf(interproRow.get("interpro_end_min").toString());
        /*
         * The start and end coordinates are on the protein,
         * but for identical positions, the rows get merged.
         * Hence 'transcript_ids' that we split.
         * Example: PF3D7_0108400
         */
        for (String transcriptId : interproRow.get("transcript_ids").toString().split(", ")){
          String chrom = transcriptId;
          StringBuilder defline = new StringBuilder(transcriptId + "::" + interproRow.get("interpro_primary_id").toString());
          if (!_useShortDefline) {
            defline.append("  | ");
            defline.append(organism);
            defline.append(" | protein | ");
            defline.append(interproRow.get("interpro_name").toString());
            defline.append(" | ");
            defline.append(interproRow.get("interpro_desc").toString());
            defline.append(" | ");
            defline.append(chrom);
            defline.append(", ");
            defline.append(""+start);
            defline.append(" to ");
            defline.append(""+end);
            defline.append(" | segment_length=");
            defline.append(""+(end - start + 1));
          }

          result.add(List.of(chrom, ""+start, ""+end, defline.toString(), ".", "."));
        }
      }

      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }

}
