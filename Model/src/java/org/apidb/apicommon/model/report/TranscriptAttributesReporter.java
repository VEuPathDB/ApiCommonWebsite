package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.FilterOptionList;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.report.AttributesTabularReporter;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

public class TranscriptAttributesReporter extends AttributesTabularReporter {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(TranscriptAttributesReporter.class);

  public static final String PROP_APPLY_FILTER = "applyFilter";
  private Boolean applyFilter;

  public TranscriptAttributesReporter(AnswerValue answerValue) {
    super(answerValue);
  } 

  @Override
  public void configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void configure(JSONObject config) throws WdkUserException {
    super.configure(config);
    try {
      applyFilter = config.getBoolean(PROP_APPLY_FILTER);
    }
    catch (JSONException e) {
      throw new WdkUserException("Missing required reporter property (boolean): " + PROP_APPLY_FILTER); 
    }
  }

  /**
   * Create a new AnswerValue, and apply filter, if user has asked for the filter
   */
  @Override
  public void initialize() throws WdkModelException {
    if (!applyFilter)
      return; // use existing answer value

    // need to create new base answervalue and apply transcript filter
    _baseAnswer = new AnswerValue(_baseAnswer);
    Question question = getQuestion();
    String filterName = RepresentativeTranscriptFilter.FILTER_NAME;
    if (question.getFilter(filterName) == null)
      throw new WdkModelException("Can't find transcript filter with name " + filterName + " on question " + question.getFullName());
    JSONObject jsFilterValue = new JSONObject(); // this filter has no params, so this stays empty
    FilterOptionList optionsList = new FilterOptionList(_wdkModel, question.getFullName());
    optionsList.addFilterOption(filterName, jsFilterValue);
    _baseAnswer.setViewFilterOptions(optionsList);
  }
}
