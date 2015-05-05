package org.apidb.apicommon.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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

public class GeneBooleanFilter extends StepFilter {
	
	protected static final String COLUMN_COUNT = "count";
	  private static final String GENE_BOOLEAN_FILTER_ARRAY_KEY = "gene_boolean_filter_array";

	public GeneBooleanFilter() {
		super("geneBooleanFilter");
	}
       
        @Override
	    public String getKey() { return GENE_BOOLEAN_FILTER_ARRAY_KEY; }

	@Override
	public FilterSummary getSummary(AnswerValue answer, String idSql)
			throws WdkModelException, WdkUserException {

		Map<String, Integer> counts = new LinkedHashMap<>();
		// group by the query and get a count
				
		// the input idSql has filters applied, and they might strip off dyn columns.  join those back in using the original id sql
		String fullIdSql = getFullSql(answer, idSql);

		String sql = getSummarySql(fullIdSql);

		ResultSet resultSet = null;
		DataSource dataSource = answer.getQuestion().getWdkModel().getAppDb().getDataSource();
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
			SqlUtils.closeResultSetAndStatement(resultSet);
		}
		return new ListColumnFilterSummary(counts);
	}
	
	private String getSummarySql(String idSql) {
		String sql = "select " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", " + TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + ", count(*) as " + COLUMN_COUNT +
				" from (" + idSql + ") group by " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", " + TranscriptBooleanQuery.RIGHT_MATCH_COLUMN ;
		
		return sql;
	}
	
	 @Override
	  public String getDisplay() {
	    return "dont care";   // custom view will take care of this
	  }

	@Override
	public String getDisplayValue(AnswerValue answer, JSONObject jsValue)
			throws WdkModelException, WdkUserException {
		return "dont care";
	}

	/**
	 * Expected JSON is: { gene_boolean_filter_array:[[Y,N], [Y,Y]] }
	 */
	@Override
	public String getSql(AnswerValue answer, String idSql, JSONObject jsValue)
			throws WdkModelException, WdkUserException {
		
		// the input idSql has filters applied, and they might strip off dyn columns.  join those back in using the original id sql
		String fullIdSql = getFullSql(answer, idSql);
		
		StringBuilder sql = new StringBuilder("select * from (" + fullIdSql + ") WHERE 1 = 0 "); // add a fake where to make the concatenation easier

		  try {
			  JSONArray jsArray = jsValue.getJSONArray(GENE_BOOLEAN_FILTER_ARRAY_KEY);
			  for (int i = 0; i < jsArray.length(); i++) {
				  JSONArray pair = jsArray.getJSONArray(i);
				  sql.append("OR (" +  TranscriptBooleanQuery.LEFT_MATCH_COLUMN + "= " + pair.getString(0) + " AND " + TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + "= " + pair.getString(1));
			  }
		  } catch (JSONException ex) {
			  throw new WdkModelException(ex);
		  }
		  return sql.toString();
	  }
	
	/**
	 *  the input idSql has filters applied, and they might strip off dyn columns.  join those back in using the original id sql
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

}
