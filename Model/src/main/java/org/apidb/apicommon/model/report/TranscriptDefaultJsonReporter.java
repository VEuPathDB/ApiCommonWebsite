package org.apidb.apicommon.model.report;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.gusdb.wdk.model.report.reporter.DefaultJsonReporter;
import org.json.JSONObject;

public class TranscriptDefaultJsonReporter extends DefaultJsonReporter {

  private Boolean _applyFilter;

  @Override
  public TranscriptDefaultJsonReporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    super.configure(config);
    _applyFilter = RepresentativeTranscriptFilter.getApplyOneGeneFilterProp(config);
    return this;
  }
  
  /**
   * Create a new AnswerValue, and apply filter, if user has asked for the filter
   */
  @Override
  public void initialize() throws WdkModelException {
    if (_applyFilter) _baseAnswer = RepresentativeTranscriptFilter.getOneTranscriptPerGeneAnswerValue(_baseAnswer);
  }

}
