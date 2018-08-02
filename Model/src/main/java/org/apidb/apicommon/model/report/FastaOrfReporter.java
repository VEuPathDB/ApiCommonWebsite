package org.apidb.apicommon.model.report;

import org.gusdb.wdk.model.answer.factory.AnswerValue;

public class FastaOrfReporter extends FastaReporter {

  public FastaOrfReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/orfSrt";
  }

}
