package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.Filter;
import org.gusdb.wdk.model.filter.FilterOption;
import org.gusdb.wdk.model.filter.FilterOptionList;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.report.AttributesColumnarReporter;
import org.json.JSONObject;

public class TranscriptAttributesReporter extends AttributesColumnarReporter {
  
  public static final String PROP_APPLY_FILTER = "applyFilter";
  private boolean applyFilter = false;

  public TranscriptAttributesReporter(AnswerValue answerValue, int startIndex, int endIndex) {
    super(answerValue, startIndex, endIndex);
  } 
  
  @Override
  public void configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void configure(JSONObject config) {
    super.configure(config);
    applyFilter = (config.has(PROP_APPLY_FILTER) ? config.getBoolean(PROP_APPLY_FILTER) : true);
  }

  /**
   * Create a new answervalue, and apply filter, if user has asked for the filter
   */
  @Override
  public void initialize() throws WdkModelException {
    
    // no reason to create new
    if (!applyFilter) return;
    
    baseAnswer = new AnswerValue(getAnswerValue(), startIndex, endIndex);
    Question question = baseAnswer.getQuestion();
    if (!question.getRecordClass().getName().equals("TranscriptRecordClasses.TranscriptRecordClass"))
      throw new WdkModelException("Calling TranscriptAttributesReporter on " + question.getRecordClass());
    String filterName = RepresentativeTranscriptFilter.FILTER_NAME;
    Filter repTransFilter = question.getFilters().get(filterName);
    if (repTransFilter == null) throw new WdkModelException("Can't find transcript filter with name " + filterName);
    JSONObject jsFilterValue = new JSONObject(); // this filter has no params, so this stays empty
    FilterOption filterOpt = new FilterOption(question, repTransFilter, jsFilterValue, false);
    FilterOptionList optionsList = new FilterOptionList(question);
    optionsList.addFilterOption(filterOpt);
    baseAnswer.setViewFilterOptions(optionsList);
  }
  
}
