package org.apidb.apicommon.model.stepanalysis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

public class EnrichmentPluginUtil {

  // static class
  private EnrichmentPluginUtil(){}

  private static final String PVALUE_PARAM_KEY = "pValueCutoff";
  private static final String ORGANISM_PARAM_KEY = "organism";

  public static final String ORGANISM_PARAM_HELP =
      "<p>Choose an organism to run an enrichment on. To see all organisms have 'All results' filter selected</p>";
  
  public static class Option {
    private String _term;
    private String _display;
    public Option(String term) { this(term, term); }
    public Option(String term, String display) {
      _term = term; _display = display;
    }
    public String getTerm() { return _term; }
    public String getDisplay() { return _display; }
  }

  public static void validateOrganism(Map<String, String[]> formParams, AnswerValue answerValue,
      WdkModel wdkModel, ValidationErrors errors) throws WdkModelException, WdkUserException {
    String organism = getSingleAllowableValueParam(ORGANISM_PARAM_KEY, formParams, errors);
    if (!getDistinctOrgsInAnswer(answerValue, wdkModel).get(0).contains(organism)) {
      errors.addParamMessage(ORGANISM_PARAM_KEY, "Invalid value passed for Organism: '" + organism + "' does not appear in this result.");
    }
  }

  public static void validatePValue(Map<String, String[]> formParams, ValidationErrors errors) {
    if (!formParams.containsKey(PVALUE_PARAM_KEY)) {
      errors.addParamMessage(PVALUE_PARAM_KEY, "Missing required parameter.");
    }
    else {
      try {
        float pValueCutoff = Float.parseFloat(formParams.get(PVALUE_PARAM_KEY)[0]);
        if (pValueCutoff <= 0 || pValueCutoff > 1) throw new NumberFormatException();
      }
      catch (NumberFormatException e) {
        errors.addParamMessage(PVALUE_PARAM_KEY, "Must be a number greater than 0 and less than or equal to 1.");
      }
    }
  }

  /**
   * Return value for param where only one value is allowed, but is required
   * 
   * @param paramKey key used to fetch values from param map
   * @param formParams map of form params
   * @param errors errors object to append error messages to
   * @return valid param value as String, or null if errors occurred
   */
  // @param errors may be null if the sources have been previously validated.
  public static String getSingleAllowableValueParam(String paramKey, Map<String, String[]> formParams, ValidationErrors errors) {
    String[] values = formParams.get(paramKey);
    if ((values == null || values.length != 1) && errors != null) {
      errors.addParamMessage(paramKey, "Missing required parameter, or more than one provided.");
      return null;
    }
    return values[0];
  }

  /**
   * Utility method to return multiple param values for the given key as an SQL compatible list
   * string (i.e. to be placed in an 'in' clause).  Values are assumed to be
   * Strings, and so are single-quoted.
   * 
   * @param paramKey name of parameter
   * @param formParams form params passed to this plugin
   * @param errors validation errors object to append additional errors to; note
   * this value may be null; if so, no errors will be appended
   * @return SQL compatible list string
   */
  public static String getArrayParamValueAsString(String paramKey,
      Map<String, String[]> formParams, ValidationErrors errors) {
    String[] values = formParams.get(paramKey);
    if ((values == null || values.length == 0) && errors != null) {
      errors.addParamMessage(paramKey, "Missing required parameter.");
    }
    return "'" + FormatUtil.join(values, "','") + "'";
  }

  public static String getOrgSpecificIdSql(AnswerValue answerValue, Map<String,
      String[]> params) throws WdkModelException, WdkUserException {
    // must wrap idSql with code that filters by the passed organism param
    return "SELECT ga.source_id " +
        "FROM ApidbTuning.GeneAttributes ga, " +
        "(" + answerValue.getIdSql() + ") r " +
        "where ga.source_id = r.source_id " +
        "and  ga.taxon_id = '" + params.get(ORGANISM_PARAM_KEY)[0] + "'";
  }

  public static String getPvalueCutoff(Map<String, String[]> params) {
    return params.get(PVALUE_PARAM_KEY)[0];
  }

  /**
   * Returns two lists containing the taxon_id and organism name of the distinct orgs
   * in the current AnswerValue.  The two lists are returned as a two-element list,
   * with the first element being the list of taxon_ids, and the second the org names.
   * 
   * @param answerValue answer value for the step to analyze
   * @param wdkModel WDK model object
   * @return two-element list; elements are lists of taxon_ids (index 0) and org names (index 1)
   * @throws WdkModelException
   * @throws WdkUserException
   */
  public static List<List<String>> getDistinctOrgsInAnswer(AnswerValue answerValue,
      WdkModel wdkModel) throws WdkModelException, WdkUserException {
    final List<String> taxonIds = new ArrayList<>();
    final List<String> orgNames = new ArrayList<>();
    String sql = "SELECT distinct ga.taxon_id, ga.organism " +
        "FROM ApidbTuning.GeneAttributes ga, " +
        "(" + answerValue.getIdSql() + ") r " +
        "where ga.source_id = r.source_id " +
        "order by ga.organism asc";
    DataSource ds = wdkModel.getAppDb().getDataSource();
    new SQLRunner(ds, sql).executeQuery(new ResultSetHandler() {
      @Override
      public void handleResult(ResultSet rs) throws SQLException {
        while (rs.next()) {
          taxonIds.add(rs.getString(1));
          orgNames.add(rs.getString(2));
        }}});
    return new ListBuilder<List<String>>().add(taxonIds).add(orgNames).toList();
  }

  public static List<Option> getOrgOptionList(AnswerValue answerValue,
      WdkModel wdkModel) throws WdkModelException, WdkUserException {
    List<List<String>> orgList = getDistinctOrgsInAnswer(answerValue, wdkModel);
    List<String> taxonIds = orgList.get(0);
    List<String> orgNames = orgList.get(1);
    List<Option> orgOptionList = new ArrayList<>();
    for (int i = 0; i < orgList.get(0).size(); i++) {
      orgOptionList.add(new Option(taxonIds.get(i), orgNames.get(i)));
    }
    return orgOptionList;
  }
  

  /* Don't need this check any more since allowing multiple orgs in result */
  @Deprecated
  public static void checkSingleOrgAnswerValue(AnswerValue answerValue, WdkModel wdkModel)
      throws WdkModelException, WdkUserException, IllegalAnswerValueException {
    List<String> distinctOrgs = getDistinctOrgsInAnswer(answerValue, wdkModel).get(0);
    if (distinctOrgs.size() > 1) {
      throw new IllegalAnswerValueException("Your result has genes from more than " +
          "one organism.  This enrichment analysis tool only accepts gene " +
          "lists from one organism.  Please use the Filter boxes to limit your " +
          "result to a single organism and try again.");
    }
    else if (distinctOrgs.isEmpty()) {
      throw new WdkModelException("No organisms returned from distinct orgs query.");
    }
  }
}
