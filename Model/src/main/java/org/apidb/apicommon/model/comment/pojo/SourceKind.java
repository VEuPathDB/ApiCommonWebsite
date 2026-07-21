package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The article source of an AI gene-publication run: a PubMed lookup or a
 * client-side PDF upload. Serialized to/from the wire (request
 * {@code document_type}, response {@code source.kind}) and the
 * {@code comment_ai_run.source_kind} column as its lowercase {@link #getWireValue()}.
 */
public enum SourceKind {

  PUBMED("pubmed"),
  UPLOAD("upload");

  private final String _wire;

  SourceKind(String wire) { _wire = wire; }

  /** The lowercase value used on the wire and in the {@code source_kind} column. */
  @JsonValue
  public String getWireValue() { return _wire; }

  /**
   * Parse a wire/DB value, leniently: unknown or null input returns {@code null}
   * rather than throwing, so request validation can emit the documented 400
   * ("document_type must be 'pubmed' or 'upload'") instead of a Jackson error.
   */
  @JsonCreator
  public static SourceKind fromWire(String wire) {
    if (wire == null) return null;
    String trimmed = wire.trim();
    for (SourceKind k : values())
      if (k._wire.equals(trimmed)) return k;
    return null;
  }
}
