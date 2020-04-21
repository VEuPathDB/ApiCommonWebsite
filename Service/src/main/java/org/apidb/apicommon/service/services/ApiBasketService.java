package org.apidb.apicommon.service.services;

import static org.apidb.apicommon.model.TranscriptUtil.TRANSCRIPT_RECORDCLASS;
import static org.apidb.apicommon.model.TranscriptUtil.isGeneRecordClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.ws.rs.PathParam;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.record.PrimaryKeyDefinition;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.request.user.BasketRequests.ActionType;
import org.gusdb.wdk.service.request.user.BasketRequests.BasketActions;
import org.gusdb.wdk.service.service.user.BasketService;

public class ApiBasketService extends BasketService {

  public ApiBasketService(@PathParam(USER_ID_PATH_PARAM) String userIdStr) {
    super(userIdStr);
  }

  private static RecordClass getTranscriptRecordClass(WdkModel wdkModel) {
    return wdkModel.getRecordClassByFullName(TRANSCRIPT_RECORDCLASS)
        .orElseThrow(() -> new WdkRuntimeException(TRANSCRIPT_RECORDCLASS + " does not exist in this model."));
  }
  /**
   * Convert gene requests to transcript requests
   */
  @Override
  protected RevisedRequest<BasketActions> translatePatchRequest(
      RecordClass recordClass, BasketActions actions) throws
      DataValidationException, WdkModelException {

    // custom behavior only if gene record class
    if (!isGeneRecordClass(recordClass.getFullName())) {
      return super.translatePatchRequest(recordClass, actions);
    }

    // convert gene IDs to transcript IDs if not adding from step
    if (!actions.getAction().equals(ActionType.ADD_FROM_STEP_ID)) {
      return new RevisedRequest<>(
          getTranscriptRecordClass(getWdkModel()),
          new BasketActions(actions.getAction(),
              getTranscriptRecords(actions.getIdentifiers(), recordClass)));
    }

    // request to add IDs from a gene step; transform to transcript step
    Step geneStep = getStepForCurrentUser(actions.getRequestedStepId(), ValidationLevel.RUNNABLE);

    return new RevisedRequest<>(
        getTranscriptRecordClass(getWdkModel()),
        new BasketActions(actions.getAction(), Collections.emptyList())
          .setRunnableAnswerSpec(TranscriptUtil.transformToRunnableGeneAnswerSpec(
              getWdkModel(), getSessionUser(), geneStep)));
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
            getTranscriptRecordClass(getWdkModel()),
            getTranscriptRecords(pksToQuery, recordClass)) :
        // otherwise use default
        super.translateQueryRequest(recordClass, pksToQuery);
  }

  /**
   * The request has a set of gene IDs.  Transform that into the set of all
   * transcript IDs for the genes in the original set.  The record class here
   * is Genes
   */
  private static List<PrimaryKeyValue> getTranscriptRecords(
      Collection<PrimaryKeyValue> genePkValues, RecordClass geneRecordClass)
      throws WdkModelException {

    WdkModel wdkModel = geneRecordClass.getWdkModel();
    PrimaryKeyDefinition transcriptPkDef = getTranscriptRecordClass(wdkModel).getPrimaryKeyDefinition();

    List<PrimaryKeyValue> transcriptRecords = new ArrayList<>();
    DataSource dataSource = wdkModel.getAppDb().getDataSource();

    // for up to 1000 gene IDs per batch, assemble a comma delimited string of gene IDs for an IN clause
    // for each batch, run the query to expand the transcripts
    StringBuilder geneIdsString = null;
    String pkString = Arrays.stream(transcriptPkDef.getColumnRefs()).collect(Collectors.joining(", "));
    String sql1 = "select " + pkString + " from ApiDBTuning.TranscriptAttributes where gene_source_id in (";
    String sql2 = ")";
    int count = 0;
    for (PrimaryKeyValue genePkValue : genePkValues) {
      String geneSourceId = genePkValue.getValues().get("source_id");
      if (count == 0)
        geneIdsString = new StringBuilder("'" + geneSourceId + "'");
      else
        geneIdsString.append(", '" + geneSourceId + "'");

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

  private static void runBatch(DataSource dataSource, String sql, PrimaryKeyDefinition transcriptPkDef, List<PrimaryKeyValue> expandedRecords)
      throws WdkModelException {
    ResultSet resultSet = null;
    String[] pkColumns = transcriptPkDef.getColumnRefs();
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, "expand-basket-to-transcripts");
      while (resultSet.next()) {
        Map<String,Object> pkValues = new HashMap<>();
        for (int i = 0; i < pkColumns.length; i++) {
          pkValues.put(pkColumns[i], resultSet.getString(pkColumns[i]));
        }
        expandedRecords.add(new PrimaryKeyValue(transcriptPkDef, pkValues));
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
