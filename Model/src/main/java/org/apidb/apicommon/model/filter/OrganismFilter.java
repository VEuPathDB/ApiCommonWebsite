package org.apidb.apicommon.model.filter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OrganismFilter extends StepFilter {
  protected static final String ORGANISM = "organism";
  protected static final String FILTER_NAME = "organismFilter";
  protected static final String FILTER_NAME_COLUMN = "filter_name";
  protected static final String FILTER_SIZE_COLUMN = "count";

  public OrganismFilter() {
	super(FILTER_NAME);
  }
  
  /**
   * This value is used in the JSON AnswerService as the filter 'name'
   */
  @Override
  public String getKey() {
    return FILTER_NAME;
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException, WdkUserException {
	// TODO Auto-generated method stub
	return null;
  }

  /**
   * Returns a JSON object with organisms and counts for the current list of ids.
   */
  @Override
  public JSONObject getSummaryJson(AnswerValue answer, String idSql) throws WdkModelException, WdkUserException {
	String sql = "(" + getFullSql(answer, idSql) + ")";	
    String organismList = answer.getIdsQueryInstance().getParamStableValues().get(ORGANISM);
    List<String> selectedOrganisms = Arrays.asList(organismList.split(","));
    Map<String, Integer> counts = getCounts(answer, sql);
    JSONArray jsonArray = new JSONArray();
    for(String selectedOrganism : counts.keySet()) {
      if(selectedOrganisms.contains(selectedOrganism)) {
        jsonArray.put(new JSONObject().append(selectedOrganism, counts.get(selectedOrganism)));
      }  
    }
    return new JSONObject().append("Results", jsonArray);
  }
  
  /**
   * Returns the sql needed to further refine the result set by the organism filter.
   */
  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue)
		throws WdkModelException, WdkUserException {
	String fullIdSql = getFullSql(answer, idSql);
	StringBuilder sql = new StringBuilder("SELECT * FROM (" + fullIdSql + ") WHERE 1 = 0 ");
    try {
      JSONArray jsArray = jsValue.getJSONArray(ORGANISM);
      for (int i = 0; i < jsArray.length(); i++) {
        String value = jsArray.getString(i);
        sql.append(" OR " + ORGANISM + " LIKE '" + value + "'");
      }
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
    return sql.toString();
  }

  /**
   * Returns a map of organisms and their counts.
   * @param answer
   * @param idSql
   * @return
   * @throws WdkModelException
   * @throws WdkUserException
   */
  protected Map<String, Integer> getCounts(AnswerValue answer, String idSql) throws WdkModelException, WdkUserException {
	WdkModel wdkModel = answer.getQuestion().getWdkModel();
	String sql = ((SqlQuery)getSummaryQuery()).getSql().replace(Utilities.MACRO_ID_SQL, idSql);
    final Map<String, Integer> querySizes = new HashMap<>();
    new SQLRunner(wdkModel.getAppDb().getDataSource(), sql, getSummaryQuery().getName())
     .executeQuery(new ResultSetHandler() {
      @Override
      public void handleResult(ResultSet rs) throws SQLException {
        while (rs.next()) {
          querySizes.put(rs.getString(FILTER_NAME_COLUMN), rs.getInt(FILTER_SIZE_COLUMN));
		}
      }
    });
    return querySizes;
  }
  
  /**
   * The sql statement constructed here adds in the organism to the ids so
   * that the wrapping sql can cull out selected organisms.
   * @param answer
   * @param idSql
   * @return
   * @throws WdkModelException
   * @throws WdkUserException
   */
  protected String getFullSql(AnswerValue answer, String idSql) throws WdkModelException, WdkUserException {
    String originalIdSql = answer.getIdsQueryInstance().getSql();
    return  "SELECT idsql.*, ga.organism AS " + ORGANISM +
			" FROM (" + originalIdSql + ") idsql, (" + idSql + ") filteredIdSql, " +
	        "  (SELECT * FROM apidbTuning.GeneAttributes) ga " +
	        "   WHERE idSql.source_id = filteredIdSql.source_id " +
            "    AND idSql.gene_source_id = filteredIdSql.gene_source_id " +
	        "    AND idSql.project_id = filteredIdSql.project_id " +
	        "    AND ga.source_id = idSql.gene_source_id " +
	        "    AND ga.project_id = idSql.project_id";
  }

  @Override
  public boolean defaultValueEquals(Step step, JSONObject value) throws WdkModelException {
  	// TODO Auto-generated method stub
  	return false;
  }

  /**
   * This filter is expected to be used ONLY as a web service.  Consequently the method
   * getSummaryJSON only is implemented.
   */
  @Override
  public FilterSummary getSummary(AnswerValue answer, String idSql) throws WdkModelException, WdkUserException {
	// TODO Auto-generated method stub
	return null;
  }

}
