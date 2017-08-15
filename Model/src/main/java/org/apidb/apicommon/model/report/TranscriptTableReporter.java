package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.TableTabularReporter;
import org.json.JSONObject;

public class TranscriptTableReporter extends TableTabularReporter {

  private static final String PROP_STEP_ID = "stepId";

  private String _originalQuestionName;

  public TranscriptTableReporter(AnswerValue answerValue) {
    super(answerValue);
  } 

  @Override
  public TranscriptTableReporter configure(Map<String, String> config) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TranscriptTableReporter configure(JSONObject config) throws WdkUserException {
    String stepId = config.getString(PROP_STEP_ID);
    if (!FormatUtil.isInteger(stepId)) {
      throw new WdkUserException("Property '" + PROP_STEP_ID + "' must be an integer.");
    }
    _originalQuestionName = _baseAnswer.getQuestion().getName();
    _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer, Integer.parseInt(stepId));
    // now that base answer is a Gene answer, check and assign selected table field name
    super.configure(config);
    return this;
  }

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
