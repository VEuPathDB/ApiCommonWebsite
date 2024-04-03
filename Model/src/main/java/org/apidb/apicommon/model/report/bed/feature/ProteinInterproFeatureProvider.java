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
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.json.JSONObject;

public class ProteinInterproFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";
  private static final String TABLE_INTERPRO = "InterPro";

  private final RequestedDeflineFields _requestedDeflineFields;

  public ProteinInterproFeatureProvider(JSONObject config) {
    _requestedDeflineFields = new RequestedDeflineFields(config);
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
      for (Map<String, AttributeValue> interproRow : interproRows) {
        Integer segmentStart = Integer.valueOf(interproRow.get("interpro_start_min").toString());
        Integer segmentEnd = Integer.valueOf(interproRow.get("interpro_end_min").toString());
        StrandDirection strand = StrandDirection.none;
        /*
         * The segmentStart and segmentEnd coordinates are on the protein,
         * but for identical positions, the rows get merged.
         * Hence 'transcript_ids' that we split.
         * Example: PF3D7_0108400
         */
        for (String transcriptId : interproRow.get("transcript_ids").toString().split(", ")){
          String chrom = transcriptId;
          DeflineBuilder defline = new DeflineBuilder(chrom + ":" + segmentStart + "-" + segmentEnd);
          if(_requestedDeflineFields.contains("organism")){
            defline.appendRecordAttribute(record, ATTR_ORGANISM);
          }
          if(_requestedDeflineFields.contains("description")){
            defline.appendAttributeValue(interproRow.get("interpro_desc"));
          }
          if(_requestedDeflineFields.contains("position")){
            defline.appendPositionAa(chrom, segmentStart, segmentEnd);
          }
          if(_requestedDeflineFields.contains("ui_choice")){
            defline.appendValue("protein features: InterPro");
          }
          if(_requestedDeflineFields.contains("segment_length")){
            defline.appendSegmentLength(segmentStart, segmentEnd);
          }
          result.add(BedLine.bed6(chrom, segmentStart, segmentEnd, defline, strand));
        }
      }

      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }

}
