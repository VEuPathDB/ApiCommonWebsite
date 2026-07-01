package org.apidb.apicommon.service.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The POST body for {@code /user-comments/ai-gene-publication}. Snake-case keys
 * match the existing comments-service JSON convention. For the upload path the
 * front-end has already extracted the paper text client-side (MuPDF.js) and
 * computed the PDF content hash; the PDF itself never reaches the server.
 *
 * <p>NOTE: this request DTO is not enumerated in the plan's file table but is
 * implied by the documented POST body; flagged at the scaffolding review.
 * Request-shape validation will additionally be wired via {@code @InSchema}
 * against {@code apicomm/ai-gene-publication/post-request.json} in deliverable 1.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGenePublicationRequest {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Options {
    // No per-request options remain — the object is kept (and still folded into
    // the jobId digest) so a future output-affecting flag automatically
    // invalidates the cache without changing the digest formula.
    //
    // NOTE: generate_product_description removed — product descriptions are now
    // compulsory (a dedicated generatePDs stage runs on every success), so the
    // flag gated nothing. Its removal, plus the PROMPT_VERSION bump, invalidates
    // every cached run from testing.
    // NOTE: create_user_comment removed in the review-on-approval pivot — the
    // generate POST never creates a comment, so the flag gated nothing. The
    // comment is created by the separate publish endpoint on user approval.
    // NOTE: validate removed (2026-06-05) — the Python authors found the
    // verifyGeneSummary pass didn't materially improve results and wasn't worth
    // the tokens, so the back-end no longer runs a validation stage.
  }

  @JsonProperty("gene_id")
  public String geneId;

  /** {@code pubmed} | {@code upload}. */
  @JsonProperty("document_type")
  public String documentType;

  /** iff document_type == pubmed. */
  @JsonProperty("pubmed_id")
  public String pubmedId;

  /** iff document_type == upload — extracted client-side by MuPDF.js. */
  @JsonProperty("paper_text")
  public String paperText;

  /** iff document_type == upload — hex SHA-256 of the PDF bytes (Web Crypto). */
  @JsonProperty("pdf_content_sha256")
  public String pdfContentSha256;

  /** optional upload provenance. */
  @JsonProperty("external_url")
  public String externalUrl;

  /** optional upload provenance. */
  @JsonProperty("external_title")
  public String externalTitle;

  /** optional upload provenance — a PubMed id or DOI the user asserts for the PDF. */
  @JsonProperty("external_ref")
  public String externalRef;

  /** {@code pubmed} | {@code doi} — kind of {@link #externalRef}; upload path only. */
  @JsonProperty("external_ref_kind")
  public String externalRefKind;

  @JsonProperty("options")
  public Options options = new Options();
}
