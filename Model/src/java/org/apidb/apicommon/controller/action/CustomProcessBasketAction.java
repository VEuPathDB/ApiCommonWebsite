package org.apidb.apicommon.controller.action;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.controller.action.ProcessBasketAction;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.json.JSONException;

public class CustomProcessBasketAction extends ProcessBasketAction {
  
  /**
   * The request has a set of transcript IDs.  Transform that into the set of all 
   * transcript IDs for the genes in the original set.
   */
  @Override
  protected List<String[]> getRecords(HttpServletRequest request, RecordClassBean recordClass)
      throws JSONException, WdkUserException, WdkModelException {

    // construct a comma delimited string of the primary key columns for this record class
    StringBuilder pkString = new StringBuilder();
    int geneSourceIdColumn = getPrimaryKeyColString(pkString, recordClass);
   
    List<String[]> records = super.getRecords(request, recordClass);
    List<String[]> expandedRecords = new ArrayList<String[]>();

    WdkModelBean wdkModelBean = ActionUtility.getWdkModel(servlet);
    DataSource dataSource = wdkModelBean.getModel().getAppDb().getDataSource();
    
    // for up to 1000 gene IDs per batch, assemble a comma delimited string of gene IDs for an IN clause
    // for each batch, run the query to expand the transcripts
    StringBuilder geneIdsString = null;
    String sql1 = "select " + pkString + " from ApiDBTuning.TranscriptAttributes where gene_source_id in (";
    String sql2 = ")";
    int count = 0;
    for (String[] record : records) {
      if (count == 0)
        geneIdsString = new StringBuilder("'" + record[geneSourceIdColumn] + "'");
      else
        geneIdsString.append(", '" + record[geneSourceIdColumn] + "'");
      
      // run the query for this batch
      if (count++ == 1000) {
        count = 0;
        runBatch(dataSource, sql1 + geneIdsString + sql2, expandedRecords, recordClass.getPrimaryKeyColumns());
      }
    }
    if (count > 0) runBatch(dataSource, sql1 + geneIdsString + sql2, expandedRecords, recordClass.getPrimaryKeyColumns());
    
    return expandedRecords;
  }
  
  private int getPrimaryKeyColString(StringBuilder pkString, RecordClassBean recordClass) throws WdkModelException {
    String[] pkColumns = recordClass.getPrimaryKeyColumns();
    int geneSourceIdIndex =  -1;
    for (int i=0; i<pkColumns.length; i++) {
      pkString.append((i==0? "" :", ") + pkColumns[i]);
      if (pkColumns[i].toLowerCase().equals("gene_source_id")) geneSourceIdIndex = i;
    }
    if (geneSourceIdIndex == -1) throw new WdkModelException("Can't find primary key column 'gene_source_id' when trying to expand basket from genes to transcripts");
    return geneSourceIdIndex;
  }
  
  private void runBatch(DataSource dataSource, String sql, List<String[]> expandedRecords, String[] pkColumns) throws WdkModelException {
    try {
      ResultSet resultSet = SqlUtils.executeQuery(dataSource, sql, "expand-basket-to-transcripts");
      while (resultSet.next()) {
	String[] row = new String[pkColumns.length];
        for (int i = 0; i<pkColumns.length; i++) row[i] = resultSet.getString(i+1);
        expandedRecords.add(row);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException("failed running SQL to expand basket: " + sql + e);
    }
 
  }

}
