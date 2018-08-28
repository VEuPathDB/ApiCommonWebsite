package org.apidb.apicommon.model.report;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.factory.AnswerValue;
import org.gusdb.wdk.model.report.HistogramAttributeReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.json.JSONObject;

public class ApiHistogramAttributeReporter extends HistogramAttributeReporter {

  public ApiHistogramAttributeReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  @Override
  public Reporter configure(JSONObject config) throws WdkUserException, WdkModelException {
    if (TranscriptUtil.isTranscriptQuestion(_baseAnswer.getAnswerSpec().getQuestion())) {
      _baseAnswer = RepresentativeTranscriptFilter.getReplacementAnswerValue(
          _baseAnswer, config.getBoolean(RepresentativeTranscriptFilter.FILTER_NAME));
    }
    return super.configure(config);
  }
}
