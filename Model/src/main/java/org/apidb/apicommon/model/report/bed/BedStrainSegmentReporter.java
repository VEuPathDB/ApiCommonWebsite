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
 * resultType switch: the reporter's whole job is to hand the provider to
 * BedReporter.configure.  The provider only DECLARES its required record class and
 * attributes; BedReporter.configure is what validates them -- it compares
 * getRequiredRecordClassFullName() against the answer's record class and resolves each
 * declared attribute/table name through getFieldsByName, which throws
 * WdkRuntimeException for a name the record class does not have.  So a mis-named attribute
 * surfaces at configure time (first use of the reporter), not at compile time.
 */
public class BedStrainSegmentReporter extends BedReporter {

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return configure(() -> new StrainSegmentFeatureProvider(config), getContentDisposition(config));
  }

}
