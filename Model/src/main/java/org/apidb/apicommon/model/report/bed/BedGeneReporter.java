package org.apidb.apicommon.model.report.bed;

import java.util.Set;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GeneGenomicFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GeneTableFieldFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.ProteinFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.ProteinInterproFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.TranscriptBlockFeaturesProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONException;
import org.json.JSONObject;

public class BedGeneReporter extends BedReporter {

  private enum SequenceType {
    genomic,
    protein,
    protein_features,
    genomic_features,
    cds,
    transcript;
  }

  private enum ProteinFeature {
    interpro,
    signalp,
    tmhmm,
    low_complexity;
  }

  // used to create output file name (retained in case question is set to gene transform below)
  private String _originalQuestionName;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      _originalQuestionName = _baseAnswer.getAnswerSpec().getQuestion().getName();

      BedFeatureProvider featureProvider = createFeatureProvider(config);

      // convert to gene answer if feature provider requires genes
      if (TranscriptUtil.GENE_RECORDCLASS.equals(featureProvider.getRequiredRecordClassFullName())) {
        _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer);
      }

      // pass the provider to superclass; will use to build and process record stream
      return configure(featureProvider);
    }
    // catch common configuration parsing runtime exceptions and convert for 400s
    catch (JSONException | IllegalArgumentException e) {
      throw new ReporterConfigException(e.getMessage());
    }
  }

  private static BedFeatureProvider createFeatureProvider(JSONObject config) throws WdkModelException {
    SequenceType type = SequenceType.valueOf(config.getString("type"));
    switch(type){
      case genomic:
        return new GeneGenomicFeatureProvider(config);
      case protein:
        return new ProteinFeatureProvider(config);
      case protein_features:
        ProteinFeature proteinFeature = ProteinFeature.valueOf(config.getString("proteinFeature"));
        switch (proteinFeature){
          case interpro:
            return new ProteinInterproFeatureProvider(config);
          case signalp:
            return new GeneTableFieldFeatureProvider(config, "SignalP", "spf_start_min", "spf_end_max");
          case tmhmm:
            return new GeneTableFieldFeatureProvider(config, "TMHMM", "tmf_start_min", "tmf_end_max");
          case low_complexity:
            return new GeneTableFieldFeatureProvider(config, "LowComplexity", "lc_start_min", "lc_end_max");
          default:
            throw new WdkModelException(String.format("Unsupported protein feature: %s", proteinFeature.name()));
        }
      case cds:
        return new TranscriptBlockFeaturesProvider(config, Set.of("CDS"), "cds");
      case transcript:
        return new TranscriptBlockFeaturesProvider(config, Set.of("CDS", "UTR"), "transcript");
      default:
        throw new WdkModelException(String.format("Unsupported sequence type: %s", type.name()));
    }
  }

  @Override
  public String getDownloadFileName() {
    return _originalQuestionName + ".bed";
  }

}
