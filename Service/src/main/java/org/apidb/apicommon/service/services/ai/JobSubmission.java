package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable bundle of the resolved inputs for one job, produced by the
 * {@link SyncPrelude} and carried on {@link JobState} for the pipeline to read.
 *
 * <p>Everything here is identical across all followers of a {@code jobId} — by
 * construction, since these are exactly the values folded into the digest
 * (gene, sorted synonyms, source key, model, promptVersion, canonical options).
 * Follower-specific state (the user id, and the {@code comment_id} each follower
 * receives at persist time) lives on {@link JobState}, not here.
 *
 * <p>Note on {@code paperText}: for the upload path the FE-supplied text is a
 * submission input ({@link #getUploadedPaperText()}); for the pubmed path the
 * text is fetched in pipeline stage ① and is therefore a transient pipeline
 * intermediate, not part of this immutable submission.
 */
public final class JobSubmission {

  private final String _jobId;
  private final String _geneId;
  private final List<String> _synonyms;        // sorted, canonical
  private final String _sourceKind;            // 'pubmed' | 'upload'
  private final String _pubmedId;              // iff sourceKind == 'pubmed'
  private final String _pdfContentSha256;      // iff sourceKind == 'upload'
  private final String _uploadedPaperText;     // iff sourceKind == 'upload' (FE-extracted)
  private final String _externalUrl;           // optional upload provenance
  private final String _externalTitle;         // optional upload provenance
  private final AiGenePublicationRequest.Options _options;
  private final String _optionsJson;           // canonical JSON of _options (baked into jobId)
  private final String _modelName;
  private final String _promptVersion;

  /**
   * Derive the immutable submission from the validated request plus the values
   * the prelude computed (digest, resolved synonyms, model/prompt identifiers).
   */
  public JobSubmission(
      AiGenePublicationRequest request,
      String jobId,
      List<String> synonyms,
      String modelName,
      String promptVersion,
      String optionsJson) {
    _jobId = jobId;
    _geneId = request.geneId;
    _synonyms = Collections.unmodifiableList(new ArrayList<>(synonyms));
    _sourceKind = request.documentType;
    _pubmedId = request.pubmedId;
    _pdfContentSha256 = request.pdfContentSha256;
    _uploadedPaperText = request.paperText;
    _externalUrl = request.externalUrl;
    _externalTitle = request.externalTitle;
    _options = request.options;
    _optionsJson = optionsJson;
    _modelName = modelName;
    _promptVersion = promptVersion;
  }

  public String getJobId() { return _jobId; }
  public String getGeneId() { return _geneId; }
  public List<String> getSynonyms() { return _synonyms; }
  public String getSourceKind() { return _sourceKind; }
  public String getPubmedId() { return _pubmedId; }
  public String getPdfContentSha256() { return _pdfContentSha256; }
  public String getUploadedPaperText() { return _uploadedPaperText; }
  public String getExternalUrl() { return _externalUrl; }
  public String getExternalTitle() { return _externalTitle; }
  public AiGenePublicationRequest.Options getOptions() { return _options; }
  public String getOptionsJson() { return _optionsJson; }
  public String getModelName() { return _modelName; }
  public String getPromptVersion() { return _promptVersion; }
}
