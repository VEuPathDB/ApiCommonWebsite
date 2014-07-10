package org.apidb.apicommon.model.stepanalysis;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class ResultCounterPlugin extends AbstractStepAnalyzer {

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException, WdkUserException {
    Integer result = answerValue.getResultSize();
    setPersistentObject(result);
    return ExecutionStatus.COMPLETE;
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    return getPersistentObject();
  }
  
}
