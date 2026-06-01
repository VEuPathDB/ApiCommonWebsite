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
    @JsonProperty("validate")
    public boolean validate = true;

    @JsonProperty("generate_product_description")
    public boolean generateProductDescription = false;

    @JsonProperty("create_user_comment")
    public boolean createUserComment = true;
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

  @JsonProperty("options")
  public Options options = new Options();
}
