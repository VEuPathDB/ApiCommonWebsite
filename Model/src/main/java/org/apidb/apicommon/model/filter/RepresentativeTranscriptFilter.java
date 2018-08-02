package org.apidb.apicommon.model.filter;

import static org.gusdb.fgputil.functional.Functions.contains;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.validation.ValidObjectFactory;
import org.gusdb.fgputil.validation.ValidObjectFactory.SemanticallyValid;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.factory.AnswerValue;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.AnswerSpecBuilder;
import org.gusdb.wdk.model.answer.spec.FilterOption;
import org.gusdb.wdk.model.answer.spec.SimpleAnswerSpec;
import org.gusdb.wdk.model.filter.FilterOptionList;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.user.User;
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

	/* 
  // select first transcript when ordered by source_id 
  private static final String FILTER_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + ") " +
      "SELECT * FROM inputSql " +
      "WHERE SOURCE_ID IN ( " +
      "  SELECT MIN(subq_.SOURCE_ID) FROM inputSql subq_ " +
      "  GROUP BY subq_.GENE_SOURCE_ID " +
      ")";

	*/


	// select the longest transcript;  
	// return only one of them (MAX source_id) if several have the same length
  private static final String FILTER_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + ") " +
      "SELECT * FROM inputSql " +
      "WHERE SOURCE_ID IN ( " +
      "    SELECT MAX(ta.SOURCE_ID) " +
      "      KEEP (DENSE_RANK FIRST ORDER BY ta.length DESC) AS SOURCE_ID " +
      "      FROM inputSql subq_, " + ATTR_TABLE_NAME + " ta " + 
      "     WHERE ta.source_id =  subq_.source_id " +
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
  public FilterSummary getSummary(AnswerValue answer, String idSql) throws WdkModelException {
    throw new UnsupportedOperationException("This filter does not provide a FilterSummary");
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException {
    //LOG.debug("Applying Representative Transcript Filter to SQL: " + idSql);
    //LOG.debug("RESULTING IN: " + FILTER_SQL.replace(ORIG_SQL_PARAM, idSql) );

    return FILTER_SQL.replace(ORIG_SQL_PARAM, idSql);
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

  public static SemanticallyValid<AnswerSpec> applyToStepFromUserPreference(SemanticallyValid<AnswerSpec> answerSpec, User user) {
    // read from step if transcript-only filter is turned on...
    boolean filterOnInStep = contains(answerSpec.getObject().getViewFilterOptions(), option ->
        option.getKey().equals(RepresentativeTranscriptFilter.FILTER_NAME));

    boolean shouldEngageFilter = shouldEngageFilter(user);

    // use passed step value if matches preference; otherwise toggle
    if (filterOnInStep == shouldEngageFilter) {
      return answerSpec;
    }

    AnswerSpecBuilder newSpec = AnswerSpec.builder(answerSpec.getObject());
    if (shouldEngageFilter) {
      // add view filter
      newSpec.getViewFilterOptions().addFilterOption(FilterOption.builder()
          .setFilterName(RepresentativeTranscriptFilter.FILTER_NAME));
    }
    else {
      // remove view filter (already present)
      newSpec.getViewFilterOptions().removeAll(option -> option.getFilterName().equals(RepresentativeTranscriptFilter.FILTER_NAME));
    }

    return ValidObjectFactory.getSemanticallyValid(newSpec.build(ValidationLevel.SEMANTIC));
  }

  @Override
  public ValidationBundle validate(Question question, JSONObject value, ValidationLevel validationLevel) {
    // No validation needed since this filter has no configuration. Its presence is all that is required.
    return ValidationBundle.builder(validationLevel).build();
  }

  public static AnswerValue updateAnswerValue(AnswerValue answerValue, Boolean shouldEngageFilter) throws WdkModelException {
    FilterOptionList viewFilters = answerValue.getViewFilterOptions();
    boolean filterOnInAnswer =
        viewFilters.getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null;

    if (filterOnInAnswer == shouldEngageFilter) {
      return answerValue;
    }

    // Create a copy of answerValue and modify view filters appropriately
    AnswerValue newAnswerValue = new AnswerValue(answerValue);
    FilterOptionList newViewFilters = new FilterOptionList(viewFilters);

    if (shouldEngageFilter) {
      // add view filter
      newViewFilters.addFilterOption(RepresentativeTranscriptFilter.FILTER_NAME, new JSONObject());
    }
    else {
      // remove view filter (already present)
      newViewFilters.removeFilterOption(RepresentativeTranscriptFilter.FILTER_NAME);
    }

    newAnswerValue.setViewFilterOptions(newViewFilters);

    return newAnswerValue;
  }
}
