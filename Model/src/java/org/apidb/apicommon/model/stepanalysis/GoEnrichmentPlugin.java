package org.apidb.apicommon.model.stepanalysis;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class GoEnrichmentPlugin extends AbstractStepAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);
  
  private static final int VERSION = 1;
  
  @Override
  public int getAnalyzerVersion() {
    return VERSION;
  }

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger logger) throws WdkModelException {
    logger.appendLine("Starting GO Enrichment of result set");
    for (int i=0; i<30; i++) {
      try {
        Thread.sleep(1000);
        String msg = "Processed " + (i+1) + "/30 of job.";
        logger.appendLine(msg);
      }
      catch (InterruptedException e) {
        setResults("Interrupted before completion.");
        return ExecutionStatus.INTERRUPTED;
      }
    }
    logger.appendLine("Finished.");
    
    setResults("GO Enrichment has run successfully!!");
    return ExecutionStatus.COMPLETE;
  }
}
