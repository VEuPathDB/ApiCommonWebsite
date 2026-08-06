package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Read-side composite of a published comment's AI provenance, assembled by
 * joining the per-comment {@code comment_ai_provenance} row ({@code is_edited})
 * to its shared {@code comment_ai_run} cache row (article source + original AI
 * headline/content) via {@code run_job_id}. Serialized as the optional
 * {@code aiProvenance} field on a GET comment response; it is absent entirely
 * for human-written comments.
 *
 * <p>Distinct from {@link AiProvenance}, which is the write-side table row used
 * to INSERT provenance on publish — this class is never persisted, and it
 * mirrors the client {@code AiProvenance} TypeScript shape instead of the table.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiProvenanceView {

  private final boolean _edited;
  private final AiRunSource _source;
  private final String _originalHeadline;
  private final String _originalContent;

  public AiProvenanceView(boolean edited, AiRunSource source,
      String originalHeadline, String originalContent) {
    _edited = edited;
    _source = source;
    _originalHeadline = originalHeadline;
    _originalContent = originalContent;
  }

  @JsonProperty("isEdited")
  public boolean isEdited() { return _edited; }

  /**
   * The article source of the AI run, the shared {@link AiRunSource} union
   * serialized as the client's discriminated union: {@code {kind:'pubmed',
   * pubmedId}} or {@code {kind:'upload', pdfContentSha256, externalUrl?,
   * externalTitle?, externalRef?, externalRefKind?}}. The record shape plus its
   * NON_NULL inclusion drop the fields that don't apply to the active
   * {@code kind}; {@code pdfContentSha256} lets the client match an uploaded PDF
   * to an existing published comment, the upload-path analogue of matching a
   * PubMed comment by its PMID.
   */
  public AiRunSource getSource() { return _source; }

  public String getOriginalHeadline() { return _originalHeadline; }

  public String getOriginalContent() { return _originalContent; }
}
