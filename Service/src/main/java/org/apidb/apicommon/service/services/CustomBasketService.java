package org.apidb.apicommon.service.services;

import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.json.JsonIterators;
import org.gusdb.fgputil.json.JsonType;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.BasketFactory;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.annotation.PATCH;
import org.gusdb.wdk.service.request.RecordRequest;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.request.exception.RequestMisformatException;
import org.gusdb.wdk.service.request.user.BasketRequests.BasketActions;
import org.gusdb.wdk.service.service.RecordService;
import org.gusdb.wdk.service.service.user.BasketService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.apidb.apicommon.model.TranscriptUtil.TRANSCRIPT_RECORDCLASS;
import static org.apidb.apicommon.model.TranscriptUtil.isGeneRecordClass;
import static org.apidb.apicommon.model.TranscriptUtil.isTranscriptRecordClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomBasketService extends BasketService {
  private static final String BASKET_NAME_PARAM = "basketName";
  private static final String BASE_BASKET_PATH = "baskets";
  private static final String NAMED_BASKET_PATH = BASE_BASKET_PATH + "/{" + BASKET_NAME_PARAM + "}";

  public CustomBasketService(@PathParam(USER_ID_PATH_PARAM) String userIdStr) {
    super(userIdStr);
  }
  
  @PATCH
  @Path(NAMED_BASKET_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response patchBasket(@PathParam(BASKET_NAME_PARAM) String basketName, String body)
      throws WdkModelException, DataValidationException, RequestMisformatException {
	  
    try {
      User user = getPrivateRegisteredUser();
      RecordClass recordClass = RecordService.getRecordClassOrNotFound(basketName, getWdkModel());
      if(!isGeneRecordClass(recordClass.getFullName())) {
        super.patchBasket(basketName, body);
      }
      else {
    	BasketFactory factory = getWdkModel().getBasketFactory();
        BasketActions actions = new BasketActions(new JSONObject(body), recordClass);
        List<PrimaryKeyValue> pkValues = actions.getRecordsToAdd();
        List<String[]> transcriptRecords = getRecords(pkValues, recordClass);
        factory.addToBasket(user, getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS), transcriptRecords);  
        pkValues = actions.getRecordsToRemove();
        transcriptRecords = getRecords(pkValues, recordClass);
        factory.removeFromBasket(user, getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS), transcriptRecords);   
      }
      return Response.noContent().build();
    }
    catch (JSONException e) {
      throw new RequestMisformatException(e.getMessage());
    }
    catch(WdkUserException wue) {
      throw new DataValidationException(wue.getMessage());
    }
  }
  
  
  @POST
  @Path(NAMED_BASKET_PATH + "/query")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response queryBasket(@PathParam(BASKET_NAME_PARAM) String basketName, String body)
      throws WdkModelException, RequestMisformatException, DataValidationException {
    try {
      User user = getPrivateRegisteredUser();
      RecordClass recordClass = RecordService.getRecordClassOrNotFound(basketName, getWdkModel());
      if(!isGeneRecordClass(recordClass.getFullName())) {
        super.queryBasket(basketName, body);
      }
      JSONArray inputArray = new JSONArray(body);
      List<PrimaryKeyValue> genePksToQuery = new ArrayList<>();
      for (JsonType pkArray : JsonIterators.arrayIterable(inputArray)) {
        if (!pkArray.getType().equals(JsonType.ValueType.ARRAY)) {
          throw new RequestMisformatException("All input array elements must be arrays.");
        }
        genePksToQuery.add(RecordRequest.parsePrimaryKey(pkArray.getJSONArray(), recordClass));
      }
      List<String[]> transcriptRecords = getRecords(genePksToQuery, recordClass);
      
      List<Boolean> result = getWdkModel().getBasketFactory().queryBasketStatus(user, transcriptRecords, getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS));
      return Response.ok(new JSONArray(result).toString()).build();
    }
    catch (JSONException e) {
      throw new RequestMisformatException(e.getMessage());
    }
    catch(WdkUserException wue) {
      throw new DataValidationException(wue.getMessage());
    }
  }
  
  /**
   * The request has a set of gene IDs.  Transform that into the set of all 
   * transcript IDs for the genes in the original set.  The record class here
   * is Genes
   */
  protected List<String[]> getRecords(List<PrimaryKeyValue> genePkValues, RecordClass geneRecordClass)
    throws JSONException, WdkUserException, WdkModelException {

	RecordClass transcriptRecordClass = getWdkModel().getRecordClass(TRANSCRIPT_RECORDCLASS);
	String[] transcriptPkColumns =  transcriptRecordClass.getPrimaryKeyDefinition().getColumnRefs(); 
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

    List<String[]> transcriptRecords = new ArrayList<>();
    DataSource dataSource = getWdkModel().getAppDb().getDataSource();
    
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
        runBatch(dataSource, sql1 + geneIdsString + sql2, transcriptRecords, transcriptPkColumns);
      }
    }
    if (count > 0) runBatch(dataSource, sql1 + geneIdsString + sql2, transcriptRecords, transcriptPkColumns);
    
    return transcriptRecords;
  }
  
  private int getPrimaryKeyColString(StringBuilder pkString, RecordClass recordClass) throws WdkModelException {
    String[] pkColumns = recordClass.getPrimaryKeyDefinition().getColumnRefs();
    int geneSourceIdIndex =  -1;
    for (int i=0; i<pkColumns.length; i++) {
      pkString.append((i==0? "" :", ") + pkColumns[i]);
      if (pkColumns[i].toLowerCase().equals("gene_source_id")) geneSourceIdIndex = i;
    }
    if (geneSourceIdIndex == -1) throw new WdkModelException("Can't find primary key column 'source_id' when trying to expand basket from genes to transcripts");
    return geneSourceIdIndex;
  }
  
  private void runBatch(DataSource dataSource, String sql, List<String[]> expandedRecords, String[] pkColumns) throws WdkModelException {
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, "expand-basket-to-transcripts");
      while (resultSet.next()) {
	String[] row = new String[pkColumns.length];
        for (int i = 0; i<pkColumns.length; i++) row[i] = resultSet.getString(i+1);
        expandedRecords.add(row);
      }
    }
    catch (SQLException e) {
      throw new WdkModelException("failed running SQL to expand basket: " + sql + e);
    }
    finally {
      if (resultSet != null) SqlUtils.closeResultSetAndStatement(resultSet, null);
    }	 
  }
	  
}
