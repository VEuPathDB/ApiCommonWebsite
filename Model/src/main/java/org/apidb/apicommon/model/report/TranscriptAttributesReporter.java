package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.spec.FilterOptionList;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.reporter.AttributesTabularReporter;
import org.json.JSONException;
import org.json.JSONObject;

public class TranscriptAttributesReporter extends AttributesTabularReporter {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(TranscriptAttributesReporter.class);

  public static final String PROP_APPLY_FILTER = "applyFilter";
  private Boolean applyFilter;

  public TranscriptAttributesReporter(AnswerValue answerValue) {
    super(answerValue);
  } 

  @Override
  public TranscriptAttributesReporter configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TranscriptAttributesReporter configure(JSONObject config) throws ReporterConfigException {
    super.configure(config);
    try {
      applyFilter = config.getBoolean(PROP_APPLY_FILTER);
      return this;
    }
    catch (JSONException e) {
      throw new ReporterConfigException("Missing required reporter property (boolean): " + PROP_APPLY_FILTER); 
    }
  }

  /**
   * Create a new AnswerValue, and apply filter, if user has asked for the filter
   */
  @Override
  public void initialize() throws WdkModelException {
    if (!applyFilter) {
      return; // use existing answer value
    }
    // otherwise need to create new base answervalue with transcript filter applied

    Question question = getQuestion();
    String filterName = RepresentativeTranscriptFilter.FILTER_NAME;
    if (question.getFilter(filterName) == null) {
      throw new WdkModelException("Can't find transcript filter with name " +
          filterName + " on question " + question.getFullName());
    }
    JSONObject jsFilterValue = new JSONObject(); // this filter has no params, so this stays empty
    RunnableObj<AnswerSpec> modifiedSpec = AnswerSpec
        .builder(_baseAnswer.getAnswerSpec())
        .setViewFilterOptions(FilterOptionList.builder().addFilterOption(filterName, jsFilterValue))
        .buildRunnable(_baseAnswer.getUser(), _baseAnswer.getAnswerSpec().getStepContainer());
    _baseAnswer = AnswerValueFactory.makeAnswer(_baseAnswer, modifiedSpec);
  }
}
