package org.apidb.apicommon.model.stepanalysis;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class LongRunningTestPlugin extends AbstractStepAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LongRunningTestPlugin.class);

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger logger) throws WdkModelException {
    logger.appendLine("Starting Processing of Result Set");
    for (int i=0; i<30; i++) {
      try {
        Thread.sleep(1000);
        String msg = "Processed " + (i+1) + "/30 of job.";
        logger.appendLine(msg);
      }
      catch (InterruptedException e) {
        setPersistentCharData("Interrupted before completion.");
        return ExecutionStatus.INTERRUPTED;
      }
    }
    logger.appendLine("Finished.");
    
    setPersistentCharData("Long-running process has run successfully!!");
    return ExecutionStatus.COMPLETE;
  }

  @Override
  public Object getResultViewModel() {
    return getPersistentCharData();
  }
}
