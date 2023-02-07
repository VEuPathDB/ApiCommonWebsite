package org.apidb.apicommon.model.report.bed;

import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GenomicSequenceFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GenomicTableFieldFeatureProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONObject;

public class BedGenomicSequenceReporter extends BedReporter {

  private enum SequenceFeature {
    whole_sequence,
    low_complexity,
    repeats,
    tandem,
    centromere;
  }

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    return configure(() -> createFeatureProvider(config), getContentDisposition(config));
  }

  private BedFeatureProvider createFeatureProvider(JSONObject config) throws WdkModelException {
    SequenceFeature sequenceFeature = SequenceFeature.valueOf(config.getString("sequenceFeature"));
    switch (sequenceFeature){
      case whole_sequence:
        return new GenomicSequenceFeatureProvider(config);
      case low_complexity:
        return new GenomicTableFieldFeatureProvider(config, "LowComplexity", "Low Complexity Regions", "start_min", "end_max");
      case repeats:
        return new GenomicTableFieldFeatureProvider(config, "Repeats", "Repeats", "start_min", "end_max");
      case tandem:
        return new GenomicTableFieldFeatureProvider(config, "TandemRepeats", "Tandem Repeats", "start_min", "end_max");
      case centromere:
        return new GenomicTableFieldFeatureProvider(config, "Centromere", "Centromere", "start_min", "end_max");
      default:
        throw new WdkModelException(String.format("Unsupported sequence feature: %s", sequenceFeature.name()));
    }
  }
}
