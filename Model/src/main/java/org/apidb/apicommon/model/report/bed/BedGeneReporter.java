package org.apidb.apicommon.model.report.bed;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.apidb.apicommon.model.report.bed.feature.BedFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GeneModelDumpFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.GeneGenomicFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.ProteinInterproFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.ProteinSequenceFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.ProteinTableFieldFeatureProvider;
import org.apidb.apicommon.model.report.bed.feature.TranscriptBlockFeaturesProvider;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONException;
import org.json.JSONObject;

public class BedGeneReporter extends BedReporter {

  private enum SequenceType {
    dna_components,
    transcript_components,
    genomic,
    spliced_genomic,
    genomic_features,
    protein,
    protein_features;
  }

  private enum DnaComponent {
    exon,
    intron;
  }

  private enum TranscriptComponent {
    five_prime_utr,
    three_prime_utr,
    cds;
  }

  private enum SplicedGenomic {
    cds,
    transcript;
  }

  public static boolean useCoordinatesOnProteinReference(JSONObject config) throws WdkModelException {
    SequenceType type = SequenceType.valueOf(config.getString("type"));
    switch(type){
      case dna_components:
      case transcript_components:
      case genomic:
      case spliced_genomic:
      case genomic_features:
        return false;
      case protein:
      case protein_features:
        return true;
      default:
        throw new WdkModelException(String.format("Unsupported sequence type: %s", type.name()));
    }
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

      /*
       * The record can be either transcript or gene
       */
      RecordClass recordClass = getQuestion().getRecordClass();

      boolean providerNeedsGene = TranscriptUtil.isGeneRecordClass(featureProvider.getRequiredRecordClassFullName());
      boolean providerNeedsTranscript = TranscriptUtil.isTranscriptRecordClass(featureProvider.getRequiredRecordClassFullName());

      boolean isTranscriptAnswer = TranscriptUtil.isTranscriptRecordClass(recordClass);

      if (isTranscriptAnswer && providerNeedsGene) {
        _baseAnswer = TranscriptUtil.transformToGeneAnswer(_baseAnswer);
        isTranscriptAnswer = false;

      }
      else if (TranscriptUtil.isGeneRecordClass(recordClass) && providerNeedsTranscript) {
        _baseAnswer = TranscriptUtil.transformToTranscriptAnswer(_baseAnswer);
        isTranscriptAnswer = true;
      }
      
      if (isTranscriptAnswer) {
        try {
          if (config.getBoolean(RepresentativeTranscriptFilter.PROP_APPLY_FILTER))
            _baseAnswer = RepresentativeTranscriptFilter.getOneTranscriptPerGeneAnswerValue(_baseAnswer);
        }
        catch (JSONException e) {
          throw new ReporterConfigException("Missing required reporter property (boolean): " + RepresentativeTranscriptFilter.PROP_APPLY_FILTER); 
        }
      }

      // pass the provider to superclass; will use to build and process record stream
      return configure(() -> featureProvider, getContentDisposition(config));
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
        return new ProteinSequenceFeatureProvider(config);
      case protein_features:
        ProteinFeature proteinFeature = ProteinFeature.valueOf(config.getString("proteinFeature"));
        switch (proteinFeature){
          case interpro:
            return new ProteinInterproFeatureProvider(config);
          case signalp:
            return new ProteinTableFieldFeatureProvider(config, "SignalP", "spf_start_min", "spf_end_max");
          case tmhmm:
            return new ProteinTableFieldFeatureProvider(config, "TMHMM", "tmf_start_min", "tmf_end_max");
          case low_complexity:
            return new ProteinTableFieldFeatureProvider(config, "LowComplexity", "lc_start_min", "lc_end_max");
          default:
            throw new WdkModelException(String.format("Unsupported protein feature: %s", proteinFeature.name()));
        }
      case spliced_genomic:
        SplicedGenomic splicedGenomic = SplicedGenomic.valueOf(config.getString("splicedGenomic"));
        switch(splicedGenomic){
          case cds:
            return new TranscriptBlockFeaturesProvider(config, "CDS", "cds");
          case transcript:
            return new TranscriptBlockFeaturesProvider(config, "Exon", "transcript");
          default:
            throw new WdkModelException(String.format("Unsupported spliced genomic type: %s", splicedGenomic.name()));
        }
      case dna_components:
        DnaComponent dnaComponent = DnaComponent.valueOf(config.getString("dnaComponent"));
        switch (dnaComponent){
          case exon:
            return new GeneModelDumpFeatureProvider(config, "Exon sequence", "DNA component: Exon", "exon");
          case intron:
            return new GeneModelDumpFeatureProvider(config, "Intron sequence", "DNA component: Intron", "intron");
          default:
            throw new WdkModelException(String.format("Unsupported dnaComponent type: %s", dnaComponent.name()));
        }
      case transcript_components:
        TranscriptComponent transcriptComponent = TranscriptComponent.valueOf(config.getString("transcriptComponent"));
        switch(transcriptComponent){
          case five_prime_utr:
            return new GeneModelDumpFeatureProvider(config, "5' UTR sequence", "Transcript component: 5' UTR", "five_prime_utr");
          case three_prime_utr:
            return new GeneModelDumpFeatureProvider(config, "3' UTR sequence" , "Transcript component: 3' UTR", "three_prime_utr");
          case cds:
            return new GeneModelDumpFeatureProvider(config, "CDS sequence", "Transcript component: CDS", "cds");
          default:
            throw new WdkModelException(String.format("Unsupported transcriptComponent type: %s", transcriptComponent.name()));
        }
      default:
        throw new WdkModelException(String.format("Unsupported sequence type: %s", type.name()));
    }
  }

  @Override
  public String getDownloadFileName() {
    // null filename will indicate inline contentDisposition
    return _isDownload ? _originalQuestionName + ".bed" : null;
  }

}
