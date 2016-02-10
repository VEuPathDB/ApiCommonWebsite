package org.apidb.apicommon.model.report;

import org.gusdb.wdk.model.answer.AnswerValue;

public class FastaGenomicSegmentReporter extends FastaReporter {

  FastaGenomicSegmentReporter(AnswerValue answerValue, int startIndex, int endIndex) {
    super(answerValue, startIndex, endIndex);
  }

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/contigSrt";
  }

}
