package org.apidb.apicommon.model.report;

public class FastaGenomicSequenceReporter extends FastaReporter {

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/contigSrt";
  }

}
