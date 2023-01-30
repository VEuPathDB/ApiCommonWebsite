package org.apidb.apicommon.model.report;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class TranscriptGff3CachedReporter extends Gff3CachedReporter {

  private Boolean _applyFilter;

  @Override
  public TranscriptGff3CachedReporter configure(JSONObject config) throws ReporterConfigException {
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
