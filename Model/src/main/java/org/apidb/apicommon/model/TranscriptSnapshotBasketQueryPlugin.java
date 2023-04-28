package org.apidb.apicommon.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.filter.MatchedTranscriptFilter;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.wdk.model.query.Column;
import org.gusdb.wdk.model.query.param.DatasetParam;
import org.gusdb.wdk.model.user.BasketSnapshotQueryPlugin;

public class TranscriptSnapshotBasketQueryPlugin extends BasketSnapshotQueryPlugin {

  @Override
  protected List<Column> getColumns(String[] pkColumns) {
    Column matchedFilterColumn = new Column();
    matchedFilterColumn.setName(MatchedTranscriptFilter.MATCHED_RESULT_COLUMN);
    return new ListBuilder<Column>()
      .addAll(super.getColumns(pkColumns))

      // same as regular columns except to add special matched result column (value supplied below)
      .add(matchedFilterColumn)

      .toList();
  }

  @Override
  protected String getSql(String[] pkColumns, DatasetParam datasetParam) {
    return new StringBuilder()
      .append("SELECT DISTINCT ")
      .append(Arrays.stream(pkColumns).collect(Collectors.joining(", ")))

      // same as regular SQL except for the next line, adding additional column value to query result
      .append(", 'Y' as " + MatchedTranscriptFilter.MATCHED_RESULT_COLUMN)

      .append(" FROM ($$" + datasetParam.getName() + "$$) t")
      .toString();
  }
}
