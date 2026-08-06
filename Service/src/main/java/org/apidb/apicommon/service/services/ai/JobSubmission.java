package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apidb.apicommon.model.comment.pojo.AiRunSource;
import org.apidb.apicommon.model.comment.pojo.SourceKind;

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
 * <p>The article {@link #getSource()} is the persisted source provenance (the
 * same {@link AiRunSource} union stored on {@link org.apidb.apicommon.model.comment.pojo.CommentAiRun}).
 * Note on {@code uploadedPaperText}: for the upload path the FE-supplied text is a
 * submission input ({@link #getUploadedPaperText()}) but is <em>not</em> part of
 * the source union — it is transient (never persisted). For the pubmed path the
 * text is fetched in pipeline stage ① and is therefore a transient pipeline
 * intermediate, not part of this immutable submission.
 */
public final class JobSubmission {

  private final String _jobId;
  private final String _geneId;
  private final List<String> _synonyms;        // sorted, canonical
  private final AiRunSource _source;           // persisted source provenance
  private final String _uploadedPaperText;     // iff upload (FE-extracted); transient, not persisted
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
    _source = request.documentType == SourceKind.PUBMED
        ? new AiRunSource.Pubmed(request.pubmedId)
        : new AiRunSource.Upload(request.pdfContentSha256, request.externalUrl,
            request.externalTitle, request.externalRef, request.externalRefKind);
    _uploadedPaperText = request.paperText;
    _options = request.options;
    _optionsJson = optionsJson;
    _modelName = modelName;
    _promptVersion = promptVersion;
  }

  public String getJobId() { return _jobId; }
  public String getGeneId() { return _geneId; }
  public List<String> getSynonyms() { return _synonyms; }
  public AiRunSource getSource() { return _source; }
  public String getUploadedPaperText() { return _uploadedPaperText; }
  public AiGenePublicationRequest.Options getOptions() { return _options; }
  public String getOptionsJson() { return _optionsJson; }
  public String getModelName() { return _modelName; }
  public String getPromptVersion() { return _promptVersion; }
}
