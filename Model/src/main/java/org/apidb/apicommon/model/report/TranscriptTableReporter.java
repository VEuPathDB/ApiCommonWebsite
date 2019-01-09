package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.report.ReporterConfigException;
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
  public TranscriptTableReporter configure(JSONObject config) throws ReporterConfigException {
    try {
      _originalQuestionName = _baseAnswer.getAnswerSpec().getQuestion().getName();
      Step baseStep = createBaseStep(_baseAnswer);
      _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer, baseStep);
      // now that base answer is a Gene answer, check and assign selected table field name
      super.configure(config);
      return this;
    }
    catch (WdkUserException e) {
      throw new ReporterConfigException(e.getMessage());
    }
    catch (WdkModelException e) {
      throw new WdkRuntimeException("Could not create in-memory step from incoming answer spec", e);
    }
  }

  private static Step createBaseStep(AnswerValue baseAnswer) throws WdkModelException {
    AnswerSpec spec = baseAnswer.getAnswerSpec();
    Map<String, String> paramValues = spec.getQueryInstanceSpec().toMap();
    return StepUtilities.createStep(
        baseAnswer.getUser(), null,
        spec.getQuestion(), paramValues,
        spec.getLegacyFilter(), false, 0,
        spec.getFilterOptions());
  }

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
