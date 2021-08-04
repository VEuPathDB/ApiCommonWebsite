package org.apidb.apicommon.model.filter;

import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getFilterValueArray;
import static org.apidb.apicommon.model.filter.FilterValueArrayUtil.getStringSetFromValueArray;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.apidb.apicommon.model.TranscriptBooleanQuery;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.filter.ListColumnFilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.query.BooleanOperator;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.question.Question;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author steve
 * For a combined transcript record result, offer user choices of which transcripts per gene to include, from the two inputs
 */
public class GeneBooleanFilter extends StepFilter {

  protected static final String COLUMN_COUNT = "count";
  public static final String GENE_BOOLEAN_FILTER_ARRAY_KEY = "gene_boolean_filter_array";

  private static enum Value { YY, YN, NY, NN }

  @Override
  public String getKey() {
    return GENE_BOOLEAN_FILTER_ARRAY_KEY;
  }

  @Override
  public JSONObject getSummaryJson(AnswerValue answer, String idSql) throws WdkModelException {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (Value value: Value.values()) {
      counts.put(value.name(), 0);
    }

    // group by the query and get a count

    // the input idSql has filters applied, and they might strip off dyn columns. join those back in using the
    // original id sql
    String fullIdSql = getFullSql(answer, idSql);

    String sql = getSummarySql(fullIdSql);

    ResultSet resultSet = null;
    DataSource dataSource = answer.getWdkModel().getAppDb().getDataSource();
    try {
      resultSet = SqlUtils.executeQuery(dataSource, sql, getKey() + "-summary");
      while (resultSet.next()) {
        String leftValue = resultSet.getString(TranscriptBooleanQuery.LEFT_MATCH_COLUMN);
        String rightValue = resultSet.getString(TranscriptBooleanQuery.RIGHT_MATCH_COLUMN);
        int count = resultSet.getInt(COLUMN_COUNT);
        counts.put(leftValue + rightValue, count);
      }
    }
    catch (SQLException ex) {
      throw new WdkModelException(ex);
    }
    finally {
      SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
    return new ListColumnFilterSummary(counts).toJson();
  }

  private String getSummarySql(String idSql) {
    String sql = "select " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", " +
        TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + ", count(*) as " + COLUMN_COUNT + " from (" + idSql +
        ") group by " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", " +
        TranscriptBooleanQuery.RIGHT_MATCH_COLUMN;

    return sql;
  }

  @Override
  public String getDisplay() {
    return "dont care"; // custom view will take care of this
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException {
    return "dont care";
  }

  /**
   * Expected JSON is: { values:["10", "01", "11"] }
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
        sql.append("OR (" + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + "= '" + value.charAt(0) + "' AND " +
            TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + "= '" + value.charAt(1) + "') ");
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
  private String getFullSql(AnswerValue answer, String idSql) throws WdkModelException {
    String originalIdSql = answer.getIdsQueryInstance().getSql();

    return "select idsql.* from (" + originalIdSql + ") idsql, (" + idSql + ") filteredIdSql where " +
        TranscriptUtil.getTranscriptRecordClass(answer.getWdkModel())
          .getPrimaryKeyDefinition().createJoinClause("idsql", "filteredIdSql");
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

  public static JSONObject getDefaultValue(BooleanOperator booleanOperatorValue) {
    switch (booleanOperatorValue) {
      case UNION: return getFilterValueArray(Value.YY.name(), Value.YN.name(), Value.NY.name());
      case INTERSECT: return getFilterValueArray(Value.YY.name());
      case LEFT_MINUS: return getFilterValueArray(Value.YN.name());
      case RIGHT_MINUS: return getFilterValueArray(Value.NY.name());
      case RIGHT_ONLY: return getFilterValueArray(Value.YY.name(), Value.NY.name());
      case LEFT_ONLY: return getFilterValueArray(Value.YY.name(), Value.YN.name());
      default: return getFilterValueArray(Value.YY.name(), Value.YN.name(), Value.NY.name());
    }
  }

  @Override
  public JSONObject getDefaultValue(SimpleAnswerSpec spec) {
    boolean isWdkSetOperation = spec.getQueryInstanceSpec().containsKey(BooleanQuery.OPERATOR_PARAM);
    if (isWdkSetOperation) {
      return getDefaultValue(BooleanQuery.getOperator(spec.getQueryInstanceSpec(), Optional.empty()));
    }
    return null;
  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    ValidationBundleBuilder validation = ValidationBundle.builder(validationLevel);
    // TODO Validate!!
    return validation.build();
  }
}
