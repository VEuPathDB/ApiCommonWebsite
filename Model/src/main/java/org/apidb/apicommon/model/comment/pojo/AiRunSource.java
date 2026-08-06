package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The article source of a {@link CommentAiRun}, as a sealed discriminated union
 * replacing the former flat {@code source_kind} string plus its "valid-iff-kind"
 * fields. One value type is shared by every source carrier — the cache row
 * ({@link CommentAiRun}), the in-flight submission, and the read-side
 * {@link AiProvenanceView} — so the pubmed-vs-upload branch is written once, as an
 * exhaustive {@code switch}, instead of duplicated {@code "pubmed".equals(...)}
 * checks.
 *
 * <p>The union serializes as the client's discriminated union without Jackson
 * polymorphism: each record carries only its own fields, and {@link #kind()} is
 * the {@code kind} discriminator. Read-side JSON ({@link AiProvenanceView}) is
 * camelCase via Jackson; the AI job endpoints build the snake_case shape by hand.
 */
public sealed interface AiRunSource permits AiRunSource.Pubmed, AiRunSource.Upload {

  /** The source discriminator ({@code pubmed} | {@code upload}). */
  SourceKind kind();

  /** A PubMed-sourced run, identified by its PMID alone. */
  record Pubmed(String pubmedId) implements AiRunSource {
    @Override
    @JsonProperty("kind")
    public SourceKind kind() { return SourceKind.PUBMED; }
  }

  /**
   * An upload-sourced run; the {@code externalUrl}/{@code externalTitle} and the
   * asserted PMID/DOI {@code externalRef}/{@code externalRefKind} are all optional.
   * {@code @JsonInclude(NON_NULL)} drops the absent optionals so the serialized
   * shape matches the read-side client union.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Upload(String pdfContentSha256, String externalUrl, String externalTitle,
                String externalRef, ExternalRefKind externalRefKind) implements AiRunSource {
    @Override
    @JsonProperty("kind")
    public SourceKind kind() { return SourceKind.UPLOAD; }
  }
}
