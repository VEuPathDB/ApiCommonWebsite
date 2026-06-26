package org.apidb.apicommon.service.services.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pure helpers for the content-digest {@code jobId} that keys both the
 * in-memory registry and the {@code comment_ai_run} cache row.
 */
public final class JobDigest {

  /** Canonical-JSON mapper: keys sorted alphabetically, compact (no whitespace). */
  private static final ObjectMapper CANONICAL_MAPPER =
      new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  // Control-char field/list separators keep the joined string unambiguous (no
  // value contains these ASCII separators), so distinct inputs cannot collide.
  // Built from code points to avoid embedding raw control chars in source.
  private static final String FIELD_SEP = String.valueOf((char) 30); // ASCII Record Separator
  private static final String LIST_SEP  = String.valueOf((char) 31); // ASCII Unit Separator

  private JobDigest() {}

  /**
   * Render the request's {@code options} as canonical JSON: properties sorted
   * alphabetically, no whitespace, all fields present (the primitive-boolean
   * defaults normalise missing values). Folding the whole options blob into the
   * digest means any future output-affecting option is covered automatically.
   */
  public static String canonicalOptionsJson(AiGenePublicationRequest.Options options) {
    try {
      return CANONICAL_MAPPER.writeValueAsString(options);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("failed to serialise options to canonical JSON", e);
    }
  }

  /**
   * Compute {@code job_id = sha256(geneId | sortedSynonyms | sourceKey |
   * modelName | promptVersion | optionsCanonicalJson)} as lowercase hex.
   * Synonyms are sorted here so callers cannot perturb the digest by order.
   */
  public static String compute(String geneId, List<String> synonyms, String sourceKey,
      String modelName, String promptVersion, String optionsJson) {
    List<String> sorted = new ArrayList<>(synonyms);
    Collections.sort(sorted);
    String canonical = String.join(FIELD_SEP,
        geneId,
        String.join(LIST_SEP, sorted),
        sourceKey,
        modelName,
        promptVersion,
        optionsJson);
    return sha256Hex(canonical);
  }

  private static String sha256Hex(String input) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(64);
      for (byte b : hash) {
        sb.append(Character.forDigit((b >> 4) & 0xf, 16));
        sb.append(Character.forDigit(b & 0xf, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 unavailable", e);
    }
  }
}
