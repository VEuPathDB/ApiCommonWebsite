package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.bed.util.BedLine;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;
import org.apidb.apicommon.model.report.bed.util.RequestedDeflineFields;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public class GeneModelDumpFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";
  private static final String TABLE_GENE_MODEL_DUMP = "GeneModelDump";

  private final RequestedDeflineFields _requestedDeflineFields;
  private final String _descriptionString;
  private final String _uiChoiceString;
  private final String _requestedFeature;

  public GeneModelDumpFeatureProvider(JSONObject config, String descriptionString, String uiChoiceString, String requestedFeature){
    _requestedDeflineFields = new RequestedDeflineFields(config);
    _descriptionString = descriptionString;
    _uiChoiceString = uiChoiceString;
    _requestedFeature = requestedFeature;
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
      TableValue geneModelDumpRows = record.getTableValue(TABLE_GENE_MODEL_DUMP);

      for (Map<String, AttributeValue> geneModelDumpRow : geneModelDumpRows) {
        if(_requestedFeature.equals(geneModelDumpRow.get("type").toString().toLowerCase())){
          String sourceId = geneModelDumpRow.get("source_id").toString();
          String chrom = geneModelDumpRow.get("sequence_id").toString();
          Integer segmentStart = Integer.valueOf(geneModelDumpRow.get("gm_start").toString());
          Integer segmentEnd = Integer.valueOf(geneModelDumpRow.get("gm_end").toString());

          StrandDirection strand = StrandDirection.fromSign(geneModelDumpRow.get("strand").toString());

          DeflineBuilder defline = new DeflineBuilder(sourceId + "::" + _requestedFeature);
          if(_requestedDeflineFields.contains("organism")){
            defline.appendRecordAttribute(record, ATTR_ORGANISM);
          }
          if(_requestedDeflineFields.contains("description")){
            defline.appendValue(_descriptionString);
          }
          if(_requestedDeflineFields.contains("position")){
            defline.appendPosition(chrom, segmentStart, segmentEnd, strand);
          }
          if(_requestedDeflineFields.contains("ui_choice")){
            defline.appendValue(_uiChoiceString);
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


