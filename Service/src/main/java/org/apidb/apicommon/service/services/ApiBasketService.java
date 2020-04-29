package org.apidb.apicommon.service.services;

import static org.apidb.apicommon.model.TranscriptUtil.getTranscriptRecordClass;
import static org.apidb.apicommon.model.TranscriptUtil.isGeneRecordClass;
import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;
import static org.gusdb.fgputil.functional.Functions.fSwallow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.ws.rs.PathParam;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.PrimaryKeyDefinition;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.request.user.BasketRequests.ActionType;
import org.gusdb.wdk.service.request.user.BasketRequests.BasketActions;
import org.gusdb.wdk.service.service.user.BasketService;

public class ApiBasketService extends BasketService {

  public ApiBasketService(@PathParam(USER_ID_PATH_PARAM) String userIdStr) {
    super(userIdStr);
  }

  /**
   * Convert gene requests to transcript requests
   */
  @Override
  protected RevisedRequest<BasketActions> translatePatchRequest(
      RecordClass recordClass, BasketActions actions) throws
      DataValidationException, WdkModelException {

    // check for the two special cases of basket requests
    boolean isGeneBasket = isGeneRecordClass(recordClass);
    boolean isTranscriptBasket = isTranscriptRecordClass(recordClass);

    // custom behavior only if gene or transcript record class
    if (!(isGeneBasket || isTranscriptBasket)) {
      return super.translatePatchRequest(recordClass, actions);
    }

    // convert gene IDs to transcript IDs if not adding from step
    if (actions.getAction().equals(ActionType.ADD_FROM_STEP_ID)) {

      // FIXME: the code below is incomplete; for now, disallow on genes and use regular RC logic for transcripts
      if (isGeneBasket) {
        throw new DataValidationException("Add-Step-To-Basket is not yet supported for gene steps");
      }
      // will add only transcripts in the result to basket, not other transcripts of those transcripts' genes
      return super.translatePatchRequest(recordClass, actions);
      
      // TODO: write code that will:
      //   For transcripts: use code below to convert to gene result (will get all genes in the result), then follow gene logic
      //                    OR (preferable!!!) write(?) a transform from transcript -> transcript that expands to all tx of all genes
      //   For genes: transform gene -> transcript generating all transcripts for all genes in the gene result and add to transcript basket
      /*
      // get the step and make sure it returns transcripts
      RunnableObj<Step> step = getRunnableStepForCurrentUser(actions.getRequestedStepId());
      RecordClass stepRecordClass = step.get().getAnswerSpec().getQuestion().getRecordClass();
      checkStepType(step.get().getStepId(), recordClass, stepRecordClass);

      return new RevisedRequest<>(
          getTranscriptRecordClass(getWdkModel()),
          new BasketActions(actions.getAction(), Collections.emptyList())
          .setRunnableAnswerSpec(TranscriptUtil.transformToRunnableGeneAnswerSpec(
              getWdkModel(), getSessionUser(), step)));
      */
    }

    // translate IDs to gene PKs if necessary
    Collection<PrimaryKeyValue> genePrimaryKeys = isGeneBasket ? actions.getIdentifiers() :
      convertTranscriptPksToGenePks(actions.getIdentifiers());

    // now convert gene PKs into transcript PKs (all transcripts for each gene)
    return new RevisedRequest<>(
        getTranscriptRecordClass(getWdkModel()),
        new BasketActions(actions.getAction(),
            getTranscriptRecords(genePrimaryKeys, recordClass)));
  }

  private Collection<PrimaryKeyValue> convertTranscriptPksToGenePks(Collection<PrimaryKeyValue> transcriptPks) {
    RecordClass geneRecordClass = TranscriptUtil.getGeneRecordClass(getWdkModel());
    return transcriptPks.stream().map(fSwallow(transcriptPk -> new PrimaryKeyValue(
        geneRecordClass.getPrimaryKeyDefinition(),
        new MapBuilder<String,Object>()
          .put("source_id", transcriptPk.getRawValues().get("gene_source_id"))
          .put("project_id", transcriptPk.getRawValues().get("project_id"))
          .toMap()))).collect(Collectors.toSet());
  }

  @SuppressWarnings("unused") // used in commented code above
  private static void checkStepType(long stepId, RecordClass expectedRecordClass, RecordClass currentRecordClass) throws DataValidationException {
    if (!currentRecordClass.getFullName().equals(expectedRecordClass.getFullName())) {
      throw new DataValidationException("Step with ID " + stepId +
          " returns records of type " + currentRecordClass.getUrlSegment() +
          ", but must return type " + expectedRecordClass.getUrlSegment());
    }
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
