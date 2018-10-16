package org.apidb.apicommon.model.filter;

import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getFilterValueArray;
import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getStringSetFromValueArray;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.factory.AnswerValue;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.ListColumnFilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.question.Question;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MatchedTranscriptFilter extends StepFilter {

  private static final Logger logger = Logger.getLogger(MatchedTranscriptFilter.class);

  protected static final String COUNT_COLUMN = "count";
  protected static final String MATCHED_RESULT_COLUMN = "matched_result";
  public static final String MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY = "matched_transcript_filter_array";

  @Override
  public String getKey() {
    return MATCHED_TRANSCRIPT_FILTER_ARRAY_KEY;
  }

  @Override
  public FilterSummary getSummary(AnswerValue answer, String idSql) throws WdkModelException {

    Map<String, Integer> counts = new LinkedHashMap<>();
    // group by the query and get a count

    // the input idSql has filters applied, and they might strip off dyn columns. join those back in using the
    // original id sql
    String fullIdSql = getFullSql(answer, idSql);

    String sql = getSummarySql(fullIdSql);

    ResultSet resultSet = null;
    DataSource dataSource = answer.getAnswerSpec().getQuestion().getWdkModel().getAppDb().getDataSource();
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
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
    return new ListColumnFilterSummary(counts);

  }

  private String getSummarySql(String idSql) {
    String sql = "select " + MATCHED_RESULT_COLUMN +  ", count(*) as " + COUNT_COLUMN + " from (" + idSql +
        ") group by " + MATCHED_RESULT_COLUMN;

    return sql;
  }

  /**
   * Start CWL 28JUN2016
   * Commented out because actual display name is an element in the transcriptFilters.xml entry for this filter.
   *
  @Override
  public String getDisplay() {
    return "dont care"; // custom view will take care of this
  }
  * End CWL
  */

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException {
    return "dont care";
  }

  /**
   * Expected JSON is, for example: { values:["Y", "N"] }
   */
  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException {

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
   * @throws WdkModelException
   */
  private String getFullSql(AnswerValue answer, String idSql) throws WdkModelException {
    String originalIdSql = answer.getIdsQueryInstance().getSql();

    return "select idsql.* from (" + originalIdSql + ") idsql, (" + idSql + ") filteredIdSql" +
        " where idSql.source_id = filteredIdSql.source_id and idSql.gene_source_id = filteredIdSql.gene_source_id and idSql.project_id = filteredIdSql.project_id";
  }

  @Override
  public void setDefaultValue(JSONObject defaultValue) {
    _defaultValue = defaultValue;
  }

  @Override
  public boolean defaultValueEquals(SimpleAnswerSpec spec, JSONObject jsValue) throws WdkModelException {
    JSONObject defaultValue = getDefaultValue(spec);
    if (defaultValue == null && jsValue == null) return true;
    if (defaultValue == null || jsValue == null) return false;
    try {
      Set<String> set1 = getStringSetFromValueArray(jsValue);
      Set<String> set2 = getStringSetFromValueArray(defaultValue);
      return set1.equals(set2);
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
  }

  @Override
  public JSONObject getDefaultValue(SimpleAnswerSpec spec) {
    if (!spec.getQuestion().isCombined() && !spec.getQuestion().getFullName().toLowerCase().contains("basket")) {
      return getFilterValueArray("Y");
    }
    else {
      logger.debug("_____________this step DOES NOT GET THE MATCHED RESULT FILTER");
      return null;
    }
  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    ValidationBundleBuilder validation = ValidationBundle.builder(validationLevel);
    // TODO Validate!!
    return validation.build();
  }
}
