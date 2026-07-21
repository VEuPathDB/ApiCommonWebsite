package org.apidb.apicommon.service.services.ai;

import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;

import org.apidb.apicommon.model.comment.pojo.ExternalRefKind;

/**
 * Normalises and validates the optional upload-path external reference
 * (a PubMed id or a DOI). Store-and-display only: never enters the job_id
 * digest or any dedup lookup. PMID and DOI formats are structurally disjoint,
 * so the same rules drive both the FE auto-detection and this server-side check.
 */
public final class ExternalRef {

  private static final Pattern PMID = Pattern.compile("^\\d{1,9}$");
  private static final Pattern DOI = Pattern.compile("^10\\.\\d{4,9}/\\S+$");
  private static final Pattern PMID_PREFIX =
      Pattern.compile("^PMID:\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern DOI_URL_PREFIX =
      Pattern.compile("^https?://(dx\\.)?doi\\.org/", Pattern.CASE_INSENSITIVE);

  /** Normalised (ref, kind); either both set or both null. */
  public static final class Result {
    public final String ref;
    public final ExternalRefKind kind;

    Result(String ref, ExternalRefKind kind) {
      this.ref = ref;
      this.kind = kind;
    }
  }

  private ExternalRef() {}

  public static Result normalise(String rawRef, ExternalRefKind kind) {
    if (rawRef == null || rawRef.trim().isEmpty()) {
      return new Result(null, null); // blank ref → no provenance, kind ignored
    }
    String ref = rawRef.trim();

    if (kind == null)
      throw new BadRequestException(
          "external_ref_kind must be 'pubmed' or 'doi' when external_ref is present");

    switch (kind) {
      case PUBMED -> {
        String stripped = PMID_PREFIX.matcher(ref).replaceFirst("").trim();
        if (!PMID.matcher(stripped).matches())
          throw new BadRequestException(
              "external_ref is not a valid PubMed id: " + rawRef);
        return new Result(stripped, ExternalRefKind.PUBMED);
      }
      case DOI -> {
        String stripped = DOI_URL_PREFIX.matcher(ref).replaceFirst("").trim();
        if (!DOI.matcher(stripped).matches())
          throw new BadRequestException(
              "external_ref is not a valid DOI: " + rawRef);
        return new Result(stripped, ExternalRefKind.DOI);
      }
      default ->
        throw new BadRequestException(
            "external_ref_kind must be 'pubmed' or 'doi' when external_ref is present");
    }
  }
}
