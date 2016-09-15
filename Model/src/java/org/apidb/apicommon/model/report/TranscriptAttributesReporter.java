package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.FilterOptionList;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.report.AttributesTabularReporter;
import org.json.JSONException;
import org.json.JSONObject;
// import org.apache.log4j.Logger;


public class TranscriptAttributesReporter extends AttributesTabularReporter {
  // private static final Logger logger = Logger.getLogger(TranscriptAttributesReporter.class);
  
  public static final String PROP_APPLY_FILTER = "applyFilter";
  private Boolean applyFilter;

  public TranscriptAttributesReporter(AnswerValue answerValue, int startIndex, int endIndex) {
    super(answerValue, startIndex, endIndex);
  } 
  
  @Override
  public void configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void configure(JSONObject config) throws WdkModelException {
    super.configure(config);
    try {
      applyFilter = config.getBoolean(PROP_APPLY_FILTER);
    }
    catch (JSONException e) {
      throw new WdkModelException("Missing required reporter property (boolean): " + PROP_APPLY_FILTER); 
    }
  }

  /**
   * Create a new answervalue, and apply filter, if user has asked for the filter
   */
  @Override
  public void initialize() throws WdkModelException {
    if (!applyFilter)
      return; // use existing answer value

    // need to create new base answervalue and apply transcript filter
    baseAnswer = new AnswerValue(getAnswerValue(), startIndex, endIndex);
    Question question = baseAnswer.getQuestion();
    String filterName = RepresentativeTranscriptFilter.FILTER_NAME;
    if (question.getFilter(filterName) == null)
      throw new WdkModelException("Can't find transcript filter with name " + filterName + " on question " + question.getFullName());
    JSONObject jsFilterValue = new JSONObject(); // this filter has no params, so this stays empty
    FilterOptionList optionsList = new FilterOptionList(wdkModel, question.getFullName());
    optionsList.addFilterOption(filterName, jsFilterValue);
    baseAnswer.setViewFilterOptions(optionsList);
  }
  
}
