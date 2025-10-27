package org.apidb.apicommon.model.filter;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.question.Question;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Filters genes based on their presence in a given pathway.  Configuring is:
 *
 * - pathway_source/pathway_source_id: the primary key of the desired pathway
 * - exact_match_only
 * - exclude_incomplete_ec
 *
 * @author rdoherty
 */
public class GenesByPathwayFilter extends StepFilter {

  private static final Logger logger = Logger.getLogger(GenesByPathwayFilter.class);

  private static final String PATHWAY_SOURCE_PARAM_NAME = "pathway_source";
  private static final String PATHWAY_ID_PARAM_NAME = "pathway_source_id";
  private static final String EXCLUDE_INCOMPLETE_EC_PARAM_NAME = "exclude_incomplete_ec";
  private static final String EXACT_MATCH_PARAM_NAME = "exact_match_only";

  public static final String GENES_BY_PATHWAY_FILTER_ARRAY_KEY = "genesByPathway";

  private static final String FILTER_SQL = 
      "SELECT DISTINCT idq.*" +
      " FROM webready.TranscriptPathway tp, ($$id_sql$$) idq" +
      " WHERE idq.gene_source_id = tp.gene_source_id" +
      "   AND tp.pathway_source = '$$pathway_source$$'" +
      "   AND tp.pathway_source_id = '$$pathway_source_id$$'" +
      "   AND tp.complete_ec >= $$exclude_incomplete_ec$$" + 
      "   AND tp.exact_match >= $$exact_match_only$$";

  @Override
  public String getKey() {
      return GENES_BY_PATHWAY_FILTER_ARRAY_KEY;
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue)
      throws WdkModelException {
    try {
      Config config = parseConfig(jsValue);
      String rv = FILTER_SQL
        .replace("@PROJECT_ID@", answer.getRequestingUser().getWdkModel().getProjectId())
        .replace("$$id_sql$$", idSql)
        .replace("$$pathway_source$$", config.getPathwaySource())
        .replace("$$exclude_incomplete_ec$$", config.getExcludeIncomplateEc())
        .replace("$$exact_match_only$$", config.getExactMatchOnly())
        .replace("$$pathway_source_id$$", config.getPathwayId());

      logger.debug("SQL=" + rv);
      return rv;
    }
    catch (WdkUserException e) {
      // should already be validated by validate method below so should never get here
      throw new WdkModelException(e);
    }
  }

  private Config parseConfig(JSONObject jsValue) throws WdkUserException {
    try {
        if (!jsValue.has(PATHWAY_SOURCE_PARAM_NAME) || !jsValue.has(PATHWAY_ID_PARAM_NAME) ||
            !jsValue.has(EXCLUDE_INCOMPLETE_EC_PARAM_NAME) || !jsValue.has(EXACT_MATCH_PARAM_NAME)) {
        throw new WdkUserException("Pathway filter requires params: " +
            PATHWAY_SOURCE_PARAM_NAME + ", " + PATHWAY_ID_PARAM_NAME + ", " + EXCLUDE_INCOMPLETE_EC_PARAM_NAME + ", " + EXACT_MATCH_PARAM_NAME);
      }
      return new Config(
          jsValue.getString(PATHWAY_SOURCE_PARAM_NAME),
          jsValue.getString(PATHWAY_ID_PARAM_NAME),
          jsValue.getString(EXCLUDE_INCOMPLETE_EC_PARAM_NAME),
          jsValue.getString(EXACT_MATCH_PARAM_NAME));
    }
    catch (JSONException e) {
      throw new WdkUserException(e.getMessage(), e);
    }
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue)
      throws WdkModelException {
    // this filter should never be displayed
    return null;
  }

  @Override
  public JSONObject getSummaryJson(AnswerValue answer, String idSql)
      throws WdkModelException {
    // the inputs to this filter are discrete but not small; not feasible to provide summary
    return null;
  }

  @Override
  public boolean defaultValueEquals(SimpleAnswerSpec spec, JSONObject value) throws WdkModelException {
    // there is no default value for this filter
    return false;
  }

  private static class Config {

    private final String _pathwaySource;
    private final String _pathwayId;
    private final String _excludeIncompleteEc;
    private final String _exactMatchOnly;

    public Config(String pathwaySource, String pathwayId, String excludeIncompleteEc, String exactMatchOnly) { 
      _pathwaySource = pathwaySource;
      _pathwayId = pathwayId;
      _excludeIncompleteEc = excludeIncompleteEc;
      _exactMatchOnly = exactMatchOnly;
    }

    public String getPathwaySource() { return _pathwaySource; }
    public String getPathwayId() { return _pathwayId; }
    public String getExcludeIncomplateEc() { return _excludeIncompleteEc; }
    public String getExactMatchOnly() { return _exactMatchOnly; }

  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    ValidationBundleBuilder validation = ValidationBundle.builder(validationLevel);
    try {
      parseConfig(value);
    }
    catch (WdkUserException e) {
      validation.addError(e.toString());
    }
    return validation.build();
  }

}
