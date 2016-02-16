package org.apidb.apicommon.model.report;

import org.gusdb.wdk.model.answer.AnswerValue;

public class FastaGenomicSequenceReporter extends FastaReporter {

  FastaGenomicSequenceReporter(AnswerValue answerValue, int startIndex, int endIndex) {
    super(answerValue, startIndex, endIndex);
  }

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/contigSrt";
  }

}
