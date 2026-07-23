package org.apidb.apicommon.model.comment.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of the optional upload-path external reference — a PubMed id or a DOI.
 * Nullable everywhere it appears (null = "no external ref"); serialized to/from
 * the wire (request/response {@code external_ref_kind}) and the
 * {@code comment_ai_run.external_ref_kind} column as its lowercase
 * {@link #getWireValue()}.
 */
public enum ExternalRefKind {

  PUBMED("pubmed"),
  DOI("doi");

  private final String _wire;

  ExternalRefKind(String wire) { _wire = wire; }

  /** The lowercase value used on the wire and in the {@code external_ref_kind} column. */
  @JsonValue
  public String getWireValue() { return _wire; }

  /**
   * Parse a wire/DB value, leniently: unknown or null input returns {@code null}
   * rather than throwing, so {@code ExternalRef.normalise} can emit its documented
   * 400 ("external_ref_kind must be 'pubmed' or 'doi'") instead of a Jackson error.
   */
  @JsonCreator
  public static ExternalRefKind fromWire(String wire) {
    if (wire == null) return null;
    String trimmed = wire.trim();
    for (ExternalRefKind k : values())
      if (k._wire.equals(trimmed)) return k;
    return null;
  }
}
