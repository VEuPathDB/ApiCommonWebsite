package org.apidb.apicommon.model.report.bed.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public abstract class TableFieldFeatureProvider implements BedFeatureProvider {

  protected abstract List<String> createFeatureRow(
      RecordInstance record, Map<String,AttributeValue> tableRow, Integer start, Integer end) throws WdkModelException;

  private final String _tableFieldName;
  private final String _startTableAttributeName;
  private final String _endTableAttributeName;

  protected TableFieldFeatureProvider(
      String tableFieldName, String startTableAttributeName, String endTableAttributeName) {
    _tableFieldName = tableFieldName;
    _startTableAttributeName = startTableAttributeName;
    _endTableAttributeName = endTableAttributeName;
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
        result.add(createFeatureRow(record, row, start, end));
      }
      return result;
    }
    catch (WdkUserException e){
      throw new WdkModelException(e);
    }
  }
}
