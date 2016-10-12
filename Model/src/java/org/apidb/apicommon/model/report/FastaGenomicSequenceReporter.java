package org.apidb.apicommon.model.report;

import org.gusdb.wdk.model.answer.AnswerValue;

public class FastaGenomicSequenceReporter extends FastaReporter {

  public FastaGenomicSequenceReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/contigSrt";
  }

}
