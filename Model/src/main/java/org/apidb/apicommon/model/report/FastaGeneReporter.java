package org.apidb.apicommon.model.report;

public class FastaGeneReporter extends FastaReporter {

  @Override
  protected String getSrtToolUri() {
    return "/cgi-bin/geneSrt";
  }

}
