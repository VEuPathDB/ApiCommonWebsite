package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.TableTabularReporter;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.StepUtilities;
import org.json.JSONObject;

public class TranscriptTableReporter extends TableTabularReporter {

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
    try {
      _originalQuestionName = _baseAnswer.getQuestion().getName();
      Step baseStep = StepUtilities.createStep(_baseAnswer.getUser(), null, _baseAnswer, false, 0);
      _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer, baseStep.getStepId());
      // now that base answer is a Gene answer, check and assign selected table field name
      super.configure(config);
      return this;
    }
    catch (WdkModelException e) {
      throw new WdkRuntimeException("Could not create in-memory step from incoming answer spec", e);
    }
  }

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
