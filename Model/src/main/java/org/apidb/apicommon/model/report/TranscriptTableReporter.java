package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.factory.AnswerValue;
import org.gusdb.wdk.model.report.reporter.TableTabularReporter;
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
      Step baseStep = createBaseStep(_baseAnswer);
      _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer, baseStep.getStepId());
      // now that base answer is a Gene answer, check and assign selected table field name
      super.configure(config);
      return this;
    }
    catch (WdkModelException e) {
      throw new WdkRuntimeException("Could not create in-memory step from incoming answer spec", e);
    }
  }

  private static Step createBaseStep(AnswerValue baseAnswer) throws WdkModelException {
    Map<String, String> paramValues = baseAnswer.getIdsQueryInstance().getParamStableValues();
    return StepUtilities.createStep(
        baseAnswer.getUser(), null,
        baseAnswer.getQuestion(), paramValues,
        baseAnswer.getFilter(), false, 0,
        baseAnswer.getFilterOptions());
  }

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
