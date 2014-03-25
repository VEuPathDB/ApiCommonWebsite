package org.apidb.apicommon.model.stepanalysis;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class ResultCounterPlugin extends AbstractStepAnalyzer {

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException {
    setResults(String.valueOf(answerValue.getResultSize()));
    return ExecutionStatus.COMPLETE;
  }
  
}
