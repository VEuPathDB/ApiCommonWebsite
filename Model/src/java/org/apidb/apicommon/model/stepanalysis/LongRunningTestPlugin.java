package org.apidb.apicommon.model.stepanalysis;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class LongRunningTestPlugin extends AbstractStepAnalyzer {

  private static final Logger LOG = Logger.getLogger(LongRunningTestPlugin.class);

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger logger) throws WdkModelException {
    logBoth(logger, "Starting Processing of Result Set");
    for (int i = 0; i < 30; i++) {
      try {
        Thread.sleep(1000);
        logBoth(logger, "Processed " + (i+1) + "/30 of job.");
      }
      catch (InterruptedException e) {
        logBoth(logger, "Interrupted!");
        setPersistentCharData("Interrupted before completion.");
        return ExecutionStatus.INTERRUPTED;
      }
    }
    setPersistentCharData("Long-running process has run successfully!!");
    logBoth(logger, "Finished.");    
    return ExecutionStatus.COMPLETE;
  }

  private void logBoth(StatusLogger logger, String message) throws WdkModelException {
    LOG.info(message);
    logger.appendLine(message);
  }

  @Override
  public Object getResultViewModel() {
    return getPersistentCharData();
  }
}
