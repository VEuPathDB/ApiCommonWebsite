package org.apidb.apicommon.model.report;

import java.util.Map;

import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.reporter.TableTabularReporter;
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
      _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer);
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

  @Override
  public String getDownloadFileName() {
    return getDownloadFileName(_originalQuestionName);
  }
}
