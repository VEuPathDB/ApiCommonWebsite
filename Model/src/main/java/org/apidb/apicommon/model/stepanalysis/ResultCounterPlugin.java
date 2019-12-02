package org.apidb.apicommon.model.stepanalysis;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;
import org.json.JSONObject;

public class ResultCounterPlugin extends AbstractStepAnalyzer {

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException, WdkUserException {
    Integer result = answerValue.getResultSizeFactory().getResultSize();
    setPersistentObject(result);
    return ExecutionStatus.COMPLETE;
  }

  @Override
  public JSONObject getFormViewModelJson() throws WdkModelException {
    JSONObject json = new JSONObject();
    return json;
  }
  @Override
  public JSONObject getResultViewModelJson() throws WdkModelException {
    JSONObject json = new JSONObject();
    json.put("resultCount", createResultViewModel());
    return json;
  }
  
  private Integer createResultViewModel() throws WdkModelException {
    return (Integer)getPersistentObject();
  }
  
}
