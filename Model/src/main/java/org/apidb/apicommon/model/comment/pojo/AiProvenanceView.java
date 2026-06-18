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

  /**
   * The article source of the AI run, mirroring the client's discriminated
   * union: {@code {kind:'pubmed', pubmedId}} or {@code {kind:'upload',
   * externalUrl?, externalTitle?, pdfContentSha256?}}. NON_NULL inclusion drops
   * the fields that don't apply to the active {@code kind}; {@code pdfContentSha256}
   * lets the client match an uploaded PDF to an existing published comment, the
   * upload-path analogue of matching a PubMed comment by its PMID.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Source {

    private final String _kind;
    private final String _pubmedId;
    private final String _externalUrl;
    private final String _externalTitle;
    private final String _pdfContentSha256;

    private Source(String kind, String pubmedId, String externalUrl,
        String externalTitle, String pdfContentSha256) {
      _kind = kind;
      _pubmedId = pubmedId;
      _externalUrl = externalUrl;
      _externalTitle = externalTitle;
      _pdfContentSha256 = pdfContentSha256;
    }

    /** A PubMed-sourced run, identified by its PMID alone. */
    public static Source pubmed(String pubmedId) {
      return new Source("pubmed", pubmedId, null, null, null);
    }

    /** An upload-sourced run, identified by the PDF content digest; url/title optional. */
    public static Source upload(String externalUrl, String externalTitle, String pdfContentSha256) {
      return new Source("upload", null, externalUrl, externalTitle, pdfContentSha256);
    }

    public String getKind() { return _kind; }
    public String getPubmedId() { return _pubmedId; }
    public String getExternalUrl() { return _externalUrl; }
    public String getExternalTitle() { return _externalTitle; }
    public String getPdfContentSha256() { return _pdfContentSha256; }
  }

  private final boolean _edited;
  private final Source _source;
  private final String _originalHeadline;
  private final String _originalContent;

  public AiProvenanceView(boolean edited, Source source,
      String originalHeadline, String originalContent) {
    _edited = edited;
    _source = source;
    _originalHeadline = originalHeadline;
    _originalContent = originalContent;
  }

  @JsonProperty("isEdited")
  public boolean isEdited() { return _edited; }

  public Source getSource() { return _source; }

  public String getOriginalHeadline() { return _originalHeadline; }

  public String getOriginalContent() { return _originalContent; }
}
