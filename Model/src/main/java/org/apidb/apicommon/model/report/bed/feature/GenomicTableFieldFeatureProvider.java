package org.apidb.apicommon.model.report.bed.feature;

import java.util.List;
import java.util.Map;

import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public class GenomicTableFieldFeatureProvider extends TableFieldFeatureProvider {

  private final StrandDirection _longStrand;

  public GenomicTableFieldFeatureProvider(JSONObject config,
      String tableFieldName, String startTableAttributeName, String endTableAttributeName) {
    super(config, tableFieldName, startTableAttributeName, endTableAttributeName);
    _longStrand = StrandDirection.valueOf(config.getString("strand"));
  }

  @Override
  public String getRequiredRecordClassFullName() {
    return "SequenceRecordClasses.SequenceRecordClass";
  }

  @Override
  protected List<String> createFeatureRow(RecordInstance record, Map<String, AttributeValue> tableRow,
      Integer start, Integer end, String organism) throws WdkModelException {
    String featureId = getSourceId(record);
    StringBuilder defline = new StringBuilder(featureId + "::" + start + "-" + end); 
    if(!_useShortDefline){
      defline.append("  | ");
      defline.append(organism);
      defline.append(" | ");
      defline.append(_tableFieldName); 
      defline.append(" | ");
      defline.append(featureId);
      defline.append(", ");
      defline.append(start);
      defline.append(" to ");
      defline.append(end);
      defline.append(" | ");
      defline.append("sequence of ");
      defline.append(_longStrand.name());
      defline.append(" strand | segment_length=");
      defline.append(end - start + 1);
    }
    return List.of(featureId, start.toString(), end.toString(), defline.toString(), ".", _longStrand.getSign());
  }
}
