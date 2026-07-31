package org.apidb.apicommon.model.report.bed;

import org.apidb.apicommon.model.report.bed.feature.StrainSegmentFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

/**
 * BED reporter for the strain genomic segment record class.
 *
 * There is only one feature provider, so unlike BedGenomicSequenceReporter there is no
 * resultType switch: the reporter's whole job is to hand BedReporter.configure the
 * provider, which validates the record class and the declared attributes at configure
 * time (so a mis-named attribute surfaces on first use rather than at compile time).
 */
public class BedStrainSegmentReporter extends BedReporter {

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return configure(() -> new StrainSegmentFeatureProvider(config), getContentDisposition(config));
  }

}
