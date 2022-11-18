package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.json.JSONObject;

public abstract class TableFieldFeatureProvider implements BedFeatureProvider {

  private static final String ATTR_ORGANISM = "organism";

  protected abstract List<String> createFeatureRow(
      RecordInstance record, Map<String,AttributeValue> tableRow, Integer start, Integer end, String organism) throws WdkModelException;

  protected final boolean _useShortDefline;
  protected final String _tableFieldName;

  private final String _startTableAttributeName;
  private final String _endTableAttributeName;

  protected TableFieldFeatureProvider(JSONObject config,
      String tableFieldName, String startTableAttributeName, String endTableAttributeName) {
    _useShortDefline = useShortDefline(config);
    _tableFieldName = tableFieldName;
    _startTableAttributeName = startTableAttributeName;
    _endTableAttributeName = endTableAttributeName;
  }

  @Override
  public String[] getRequiredAttributeNames() {
    return new String[] { ATTR_ORGANISM };
  }

  @Override
  public String[] getRequiredTableNames() {
    return new String[] { _tableFieldName };
  }

  @Override
  public List<List<String>> getRecordAsBedFields(RecordInstance record) throws WdkModelException {
    try {
      List<List<String>> result = new ArrayList<>();
      for (Map<String, AttributeValue> row : record.getTableValue(_tableFieldName)) {
        Integer start = Integer.valueOf(row.get(_startTableAttributeName).toString());
        Integer end = Integer.valueOf(row.get(_endTableAttributeName).toString());
        String organism = stringValue(record, ATTR_ORGANISM);
        result.add(createFeatureRow(record, row, start, end, organism));
      }
      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }
}
