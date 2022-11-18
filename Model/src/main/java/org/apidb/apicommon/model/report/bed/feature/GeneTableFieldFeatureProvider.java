package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public class GeneTableFieldFeatureProvider extends TableFieldFeatureProvider {

  public GeneTableFieldFeatureProvider(JSONObject config,
      String tableFieldName, String startTableAttributeName, String endTableAttributeName) {
    super(config, tableFieldName, startTableAttributeName, endTableAttributeName);
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return TranscriptUtil.GENE_RECORDCLASS;
  }

  @Override
  protected List<String> createFeatureRow(RecordInstance record, Map<String, AttributeValue> tableRow,
      Integer start, Integer end, String organism) throws WdkModelException {
    String featureId = tableRow.get("transcript_id").toString();
    StringBuilder defline = new StringBuilder(featureId + "::" + start + "-" + end);
    if (!_useShortDefline) {
      defline.append("  | ");
      defline.append(organism);
      defline.append(" | protein | ");
      defline.append(_tableFieldName);
      defline.append(" | ");
      defline.append(featureId);
      defline.append(", ");
      defline.append(start);
      defline.append(" to ");
      defline.append(end);
      defline.append(" | segment_length=");
      defline.append(end - start + 1);
    }
    return List.of(featureId, start.toString(), end.toString(), defline.toString(), ".", ".");
  }
}
