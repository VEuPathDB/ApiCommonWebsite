package org.apidb.apicommon.model.filter;

import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONException;
import org.json.JSONObject;

public class GenesByPathwayFilter extends StepFilter {

  private static final String PATHWAY_SOURCE_PARAM_NAME = "pathway_source";
  private static final String PATHWAY_ID_PARAM_NAME = "pathway_source_id";

  private static class Config extends TwoTuple<String,String> {
    public Config(String pathwaySource, String pathwayId) { super(pathwaySource, pathwayId); }
    public String getPathwaySource() { return getFirst(); }
    public String getPathwayId() { return getSecond(); }
  }

  private static final String FILTER_SQL = 
      "SELECT DISTINCT idq.*" +
      " FROM apidbtuning.transcriptpathway tp, ($$id_sql$$) idq" +
      " WHERE idq.gene_source_id = tp.gene_source_id" +
      "   AND tp.pathway_source = '$$pathway_source$$'" +
      "   AND tp.pathway_source_id = '$$pathway_source_id$$'";

  public GenesByPathwayFilter(String name) {
    super(name);
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue)
      throws WdkModelException, WdkUserException {
    Config config = parseConfig(jsValue);
    return FILTER_SQL
        .replace("@PROJECT_ID@", answer.getUser().getWdkModel().getProjectId())
        .replace("$$id_sql$$", idSql)
        .replace("$$pathway_source$$", config.getPathwaySource())
        .replace("$$pathway_source_id$$", config.getPathwayId());
  }

  private Config parseConfig(JSONObject jsValue) throws WdkUserException {
    try {
      if (!jsValue.has(PATHWAY_SOURCE_PARAM_NAME) || !jsValue.has(PATHWAY_ID_PARAM_NAME)) {
        throw new WdkUserException("Pathway filter requires params: " +
            PATHWAY_SOURCE_PARAM_NAME + ", " + PATHWAY_ID_PARAM_NAME);
      }
      return new Config(
          jsValue.getString(PATHWAY_SOURCE_PARAM_NAME),
          jsValue.getString(PATHWAY_ID_PARAM_NAME));
    }
    catch (JSONException e) {
      throw new WdkUserException(e.getMessage(), e);
    }
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue)
      throws WdkModelException, WdkUserException {
    // this filter should never be displayed
    return null;
  }

  @Override
  public FilterSummary getSummary(AnswerValue answer, String idSql)
      throws WdkModelException, WdkUserException {
    // the inputs to this filter are discrete but not small; not feasible to provide summary
    return null;
  }

  @Override
  public boolean defaultValueEquals(Step step, JSONObject value) throws WdkModelException {
    // there is no default value for this filter
    return false;
  }

}
