package org.apidb.apicommon.model.report.bed;

import org.apidb.apicommon.model.report.bed.feature.DynSpanFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class BedDynSpanReporter extends BedReporter {

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return configure(() -> new DynSpanFeatureProvider(config), getContentDisposition(config));
  }

}
