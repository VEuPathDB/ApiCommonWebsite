package org.apidb.apicommon.model.filter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.ListColumnFilterSummary;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MatchedTranscriptFilter extends StepFilter {

  protected static final String COUNT_COLUMN = "count";
  protected static final String MATCHED_RESULT_COLUMN = "matched_result";
  private static final String MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY = "matched_transcript_filter_array";

  public MatchedTranscriptFilter() {
    super("matchedTranscriptFilter");
  }

  @Override
  public String getKey() {
    return MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY;
  }

  @Override
  public FilterSummary getSummary(AnswerValue answer, String idSql) throws WdkModelException,
      WdkUserException {

    Map<String, Integer> counts = new LinkedHashMap<>();
    // group by the query and get a count

    // the input idSql has filters applied, and they might strip off dyn columns. join those back in using the
    // original id sql
    String fullIdSql = getFullSql(answer, idSql);

    String sql = getSummarySql(fullIdSql);

    ResultSet resultSet = null;
    DataSource dataSource = answer.getQuestion().getWdkModel().getAppDb().getDataSource();
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, getKey() + "-summary");
      while (resultSet.next()) {
        String value = resultSet.getString(MATCHED_RESULT_COLUMN);
        int count = resultSet.getInt(COUNT_COLUMN);
        counts.put(value, count);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet);
    }
    return new ListColumnFilterSummary(counts);

  }

  private String getSummarySql(String idSql) {
    String sql = "select " + MATCHED_RESULT_COLUMN +  ", count(*) as " + COUNT_COLUMN + " from (" + idSql +
        ") group by " + MATCHED_RESULT_COLUMN;

    return sql;
  }

  @Override
  public String getDisplay() {
    return "dont care"; // custom view will take care of this
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException,
      WdkUserException {
    return "dont care";
  }

  /**
   * Expected JSON is, for example: { values:["Y", "N"] }
   */
  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException,
      WdkUserException {

    // the input idSql has filters applied, and they might strip off dyn columns. join those back in using the
    // original id sql
    String fullIdSql = getFullSql(answer, idSql);

    // add a fake where to make the concatenation easier
    StringBuilder sql = new StringBuilder("select * from (" + fullIdSql + ") WHERE 1 = 0 ");

    try {
      JSONArray jsArray = jsValue.getJSONArray("values");
      for (int i = 0; i < jsArray.length(); i++) {
        String value = jsArray.getString(i);
        sql.append(" OR " + MATCHED_RESULT_COLUMN + "= '" + value.charAt(0) + "'");
      }
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
    return sql.toString();
  }

  /**
   * the input idSql has filters applied, and they might strip off dyn columns. join those back in using the
   * original id sql
   * 
   * @param answer
   * @param idSql
   * @return
   * @throws WdkUserException
   * @throws WdkModelException
   */
  private String getFullSql(AnswerValue answer, String idSql) throws WdkModelException, WdkUserException {
    String originalIdSql = answer.getIdsQueryInstance().getSql();

    return "select idsql.* from (" + originalIdSql + ") idsql, (" + idSql + ") filteredIdSql" +
        " where idSql.source_id = filteredIdSql.source_id and idSql.project_id = filteredIdSql.project_id";
  }

  @Override
  public void setDefaultValue(JSONObject defaultValue) {
    _defaultValue = defaultValue;
  }

  @Override
  public boolean defaultValueEquals(JSONObject jsValue) throws WdkModelException {
    if (getDefaultValue() == null)
      return false;
    try {
      JSONArray jsArray = jsValue.getJSONArray("values");
      Set<String> set1 = getStringSetFromJSONArray(jsArray);
      jsArray = getDefaultValue().getJSONArray("values");
      Set<String> set2 = getStringSetFromJSONArray(jsArray);
      return set1.equals(set2);
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
  }

  @Override
  public JSONObject getDefaultValue() {
    JSONObject jsValue = new JSONObject();
    JSONArray jsArray = new JSONArray();
    jsArray.put("Y");
		// jsArray.put("N");
    jsValue.put("values", jsArray);
    return jsValue;
  }

  private Set<String> getStringSetFromJSONArray(JSONArray jsArray) throws JSONException {
    Set<String> set = new HashSet<String>();

    for (int i = 0; i < jsArray.length(); i++) {
      String value = jsArray.getString(i);
      set.add(value);
    }
    return set;
  }

}
