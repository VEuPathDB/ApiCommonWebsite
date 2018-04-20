package org.apidb.apicommon.service.services;

import static org.apidb.apicommon.model.TranscriptUtil.TRANSCRIPT_RECORDCLASS;
import static org.apidb.apicommon.model.TranscriptUtil.isGeneRecordClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.PathParam;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.PrimaryKeyDefinition;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.service.request.user.BasketRequests.BasketActions;
import org.gusdb.wdk.service.service.user.BasketService;

public class CustomBasketService extends BasketService {

  public CustomBasketService(@PathParam(USER_ID_PATH_PARAM) String userIdStr) {
    super(userIdStr);
  }

  /**
   * Convert gene requests to transcript requests
   */
  @Override
  protected RevisedRequest<BasketActions> translatePatchRequest(
      RecordClass recordClass, BasketActions actions) throws WdkModelException {
    return isGeneRecordClass(recordClass.getFullName()) ?
        // convert gene IDs to transcript IDs
        new RevisedRequest<>(
            recordClass.getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS),
            new BasketActions(
                getTranscriptRecords(actions.getRecordsToAdd(), recordClass),
                getTranscriptRecords(actions.getRecordsToRemove(), recordClass))) :
        // otherwise use default
        super.translatePatchRequest(recordClass, actions);
  }

  /**
   * Convert gene requests to transcript requests
   */
  @Override
  protected RevisedRequest<List<PrimaryKeyValue>> translateQueryRequest(
      RecordClass recordClass, List<PrimaryKeyValue> pksToQuery) throws WdkModelException {
    return isGeneRecordClass(recordClass.getFullName()) ?
        // convert gene IDs to transcript IDs
        new RevisedRequest<>(
            recordClass.getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS),
            getTranscriptRecords(pksToQuery, recordClass)) :
        // otherwise use default
        super.translateQueryRequest(recordClass, pksToQuery);
  }

  /**
   * The request has a set of gene IDs.  Transform that into the set of all 
   * transcript IDs for the genes in the original set.  The record class here
   * is Genes
   */
  private static List<PrimaryKeyValue> getTranscriptRecords(List<PrimaryKeyValue> genePkValues, RecordClass geneRecordClass)
      throws WdkModelException {

    WdkModel wdkModel = geneRecordClass.getWdkModel();
    RecordClass transcriptRecordClass = wdkModel.getRecordClass(TRANSCRIPT_RECORDCLASS);
    PrimaryKeyDefinition transcriptPkDef =  transcriptRecordClass.getPrimaryKeyDefinition(); 
    String[] genePkColumns = geneRecordClass.getPrimaryKeyDefinition().getColumnRefs();
    List<String[]> geneRecords = new ArrayList<>();
    String[] values = new String[genePkColumns.length];
    for (PrimaryKeyValue genePkValue : genePkValues) {
      Map<String,String> genePkMap = genePkValue.getValues();
      for (int j = 0; j < values.length; j++) {
        values[j] = genePkMap.get(genePkColumns[j]);
      }
      geneRecords.add(values);
    }

    // construct a comma delimited string of the primary key columns for this record class
    StringBuilder pkString = new StringBuilder();
    int sourceIdColumn = getPrimaryKeyColString(pkString, transcriptRecordClass);

    List<PrimaryKeyValue> transcriptRecords = new ArrayList<>();
    DataSource dataSource = wdkModel.getAppDb().getDataSource();

    // for up to 1000 gene IDs per batch, assemble a comma delimited string of gene IDs for an IN clause
    // for each batch, run the query to expand the transcripts
    StringBuilder geneIdsString = null;
    String sql1 = "select " + pkString + " from ApiDBTuning.TranscriptAttributes where gene_source_id in (";
    String sql2 = ")";
    int count = 0;
    for (String[] geneRecord : geneRecords) {
      if (count == 0)
        geneIdsString = new StringBuilder("'" + geneRecord[sourceIdColumn] + "'");
      else
        geneIdsString.append(", '" + geneRecord[sourceIdColumn] + "'");
      
      // run the query for this batch
      if (count++ == 1000) {
        count = 0;
        runBatch(dataSource, sql1 + geneIdsString + sql2, transcriptPkDef, transcriptRecords);
      }
    }
    if (count > 0) {
      runBatch(dataSource, sql1 + geneIdsString + sql2, transcriptPkDef, transcriptRecords);
    }
    return transcriptRecords;
  }

  private static int getPrimaryKeyColString(StringBuilder pkString, RecordClass recordClass) throws WdkModelException {
    String[] pkColumns = recordClass.getPrimaryKeyDefinition().getColumnRefs();
    int geneSourceIdIndex =  -1;
    for (int i=0; i<pkColumns.length; i++) {
      pkString.append((i==0? "" :", ") + pkColumns[i]);
      if (pkColumns[i].toLowerCase().equals("gene_source_id")) geneSourceIdIndex = i;
    }
    if (geneSourceIdIndex == -1) throw new WdkModelException("Can't find primary key column 'source_id' when trying to expand basket from genes to transcripts");
    return geneSourceIdIndex;
  }

  private static void runBatch(DataSource dataSource, String sql, PrimaryKeyDefinition pkDef, List<PrimaryKeyValue> expandedRecords)
      throws WdkModelException {
    ResultSet resultSet = null;
    String[] pkColumns = pkDef.getColumnRefs();
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, "expand-basket-to-transcripts");
      while (resultSet.next()) {
        Map<String,Object> pkValues = new HashMap<>();
        for (int i = 0; i < pkColumns.length; i++) {
          pkValues.put(pkColumns[i], resultSet.getString(pkColumns[i]));
        }
        expandedRecords.add(new PrimaryKeyValue(pkDef, pkValues));
      }
    }
    catch (SQLException e) {
      throw new WdkModelException("failed running SQL to expand basket: " + sql + e);
    }
    finally {
      if (resultSet != null) {
        SqlUtils.closeResultSetAndStatement(resultSet, null);
      }
    }
  }

}
