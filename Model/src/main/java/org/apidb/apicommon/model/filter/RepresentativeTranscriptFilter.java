package org.apidb.apicommon.model.filter;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.AnswerSpecBuilder;
import org.gusdb.wdk.model.answer.spec.FilterOption;
import org.gusdb.wdk.model.answer.spec.FilterOptionList;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.answer.spec.FilterOptionList.FilterOptionListBuilder;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.request.exception.DataValidationException;
import org.gusdb.wdk.service.service.AnswerService;
import org.json.JSONObject;

public class RepresentativeTranscriptFilter extends StepFilter {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(RepresentativeTranscriptFilter.class);

  private static final boolean REPRESENTATIVE_TRANSCRIPT_FILTER_ON_BY_DEFAULT = false;

  /**
   * The following String value is used in a variety of places.  Do not change
   *      unless you know what you're doing.  Here's where it is used: <br/>
   * <ol>
   *   <li>Name of the representative-transcript-only view filter (map key in Java code)</li>
   *   <li>Name of the JSON property in the database within the view-only filter property value object</li>
   *   <li>Name of the request-scope JSP-EL variable for use by the transcript summary view JSP</li>
   *   <li>Name of the JSON property in the POST service request body used to change the value</li>
   * </ol>
   */
  public static final String FILTER_NAME = "representativeTranscriptOnly";

  public static final String ATTR_TABLE_NAME = "ApiDBTuning.TranscriptAttributes";

  private static final String ORIG_SQL_PARAM = "%%originalSql%%";

  private static final String LONGEST_TRANSCRIPT_REQUIRED_OPTION = "longestTranscriptRequired";

  // select first transcript when ordered by source_id 
  private static final String SELECT_FIRST_TRANSCRIPT_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + ") " +
      "SELECT * FROM inputSql " +
      "WHERE SOURCE_ID IN ( " +
      "  SELECT MIN(subq_.SOURCE_ID) FROM inputSql subq_ " +
      "  GROUP BY subq_.GENE_SOURCE_ID " +
      ")";

  // select the longest transcript;  
  // return only one of them (MAX source_id) if several have the same length
  private static final String SELECT_LONGEST_TRANSCRIPT_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + ") " +
      "SELECT * FROM inputSql " +
      "WHERE SOURCE_ID IN ( " +
      "  SELECT MAX(ta.SOURCE_ID) " +
      "    KEEP (DENSE_RANK FIRST ORDER BY ta.length DESC) AS SOURCE_ID " +
      "    FROM inputSql subq_, " + ATTR_TABLE_NAME + " ta " + 
      "    WHERE ta.source_id =  subq_.source_id " +
      "  GROUP BY subq_.GENE_SOURCE_ID " +
      ")";


  /*
  // select the longest transcript:  returns multiple if same length
  private static final String FILTER_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + "), " +
      " inputSql2 as " +
      " ( SELECT inputSql.*, ta.length " +
      "     FROM inputSql, " + ATTR_TABLE_NAME + " ta " +  
      "    WHERE inputSql.source_id = ta.source_id )" +
      " SELECT is21.* " +
      "   FROM inputSql2 is21 " +
      "        LEFT OUTER JOIN inputSql2 is22 " +
      "        ON (is21.gene_source_id = is22.gene_source_id " + 
      "             AND " +
      "            is21.length < is22.length) " +
      "  WHERE is22.gene_source_id IS NULL ";
  */

  @Override
  public String getKey() {
    return FILTER_NAME;
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException {
    return "Shows only a representative transcript for each gene.";
  }

  @Override
  public JSONObject getSummaryJson(AnswerValue answer, String idSql) throws WdkModelException {
    throw new UnsupportedOperationException("This filter does not provide a FilterSummary");
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException {
    String filterSql = jsValue.optBoolean(LONGEST_TRANSCRIPT_REQUIRED_OPTION, true)
        ? SELECT_LONGEST_TRANSCRIPT_SQL : SELECT_FIRST_TRANSCRIPT_SQL;
    return filterSql.replace(ORIG_SQL_PARAM, idSql);
  }

  @Override
  public boolean defaultValueEquals(SimpleAnswerSpec answerSpec, JSONObject value) throws WdkModelException {
    return false;
  }

  public static boolean shouldEngageFilter(User user) {
    // get user preference
    String prefValue = user.getPreferences().getProjectPreference(RepresentativeTranscriptFilter.FILTER_NAME);
    return (prefValue == null ? REPRESENTATIVE_TRANSCRIPT_FILTER_ON_BY_DEFAULT : Boolean.valueOf(prefValue));
  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    // No validation needed since this filter has no configuration. Its presence is all that is required.
    return ValidationBundle.builder(validationLevel).build();
  }

  /**
   * get an answer value that applies the view-only filter that reduces the result to one
   * transcript per gene
   */
  public static AnswerValue applyRepresentativeTranscriptFilter(AnswerValue originalAnswer, boolean longestTranscriptRequired) throws WdkModelException {
    AnswerSpec originalAnswerSpec = originalAnswer.getAnswerSpec();
    AnswerSpecBuilder specBuilder = AnswerSpec.builder(originalAnswerSpec);
    FilterOptionListBuilder viewFiltersBuilder = FilterOptionList.builder()
      .addAllFilters(originalAnswerSpec.getViewFilterOptions())
      .addFilterOption(FilterOption.builder()
        .setFilterName(RepresentativeTranscriptFilter.FILTER_NAME)
        .setValue(new JSONObject()
            .put(LONGEST_TRANSCRIPT_REQUIRED_OPTION, longestTranscriptRequired))
        .setDisabled(false));
    specBuilder.setViewFilterOptions(viewFiltersBuilder);

    try {
      RunnableObj<AnswerSpec> runnableSpec = specBuilder.buildRunnable(originalAnswer.getUser(),
          AnswerService.loadContainer(specBuilder, originalAnswer.getWdkModel(), originalAnswer.getUser()));
      return AnswerValueFactory.makeAnswer(originalAnswer, runnableSpec);
    }
    catch (DataValidationException e) {
      throw new WdkModelException(e);
    }
  }

}
