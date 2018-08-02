package org.apidb.apicommon.model.report;

import org.gusdb.wdk.model.answer.factory.AnswerValue;

public class FastaGeneReporter extends FastaReporter {

  public FastaGeneReporter(AnswerValue answerValue) {
    super(answerValue);
  }

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/geneSrt";
  }

}
