package org.apidb.apicommon.model.report;

public class FastaOrfReporter extends FastaReporter {

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/orfSrt";
  }

}
