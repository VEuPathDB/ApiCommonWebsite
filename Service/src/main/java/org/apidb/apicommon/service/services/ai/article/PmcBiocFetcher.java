package org.apidb.apicommon.service.services.ai.article;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stage ① article-text resolution for the PubMed path: fetches the PMC BioC
 * JSON document for a PubMed id, keeps passages whose
 * {@code infons.section_type} is one of {FIG, TABLE, RESULTS, CONCL,
 * DISCUSSION, SUPPL}, and concatenates their {@code text}.
 *
 * <p>A non-JSON / 404 / non-open-access response yields a terminal
 * {@code text-unavailable} (never persisted to the cache — retries re-run the
 * fetch). The upload path has no fetch: the front-end-supplied {@code paper_text}
 * is used directly.
 *
 * <p>Ported from Python {@code get_pubmed_json} / {@code parse_pubmed_json}
 * (PubGene_back_end/helpers.py).
 */
public class PmcBiocFetcher {

  public static final String BIOC_URL_BASE =
      "https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/";

  /**
   * Passage section types whose text we keep. Mirrors Python {@code PUBMED_SECTIONS},
   * but corrects {@code DISCUSSION} → {@code DISCUSS}: the real PMC BioC vocabulary
   * emits {@code DISCUSS}, so the Python token matched nothing and silently dropped
   * every discussion section. (Reported upstream — see CLAUDE-ai-user-comments.md.)
   */
  static final Set<String> RELEVANT_SECTIONS = new HashSet<>(Arrays.asList(
      "FIG", "TABLE", "RESULTS", "CONCL", "DISCUSS", "SUPPL"));

  /** Matches Python {@code HTTP_TIMEOUT} default (180 s). */
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(180);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpClient _http;

  public PmcBiocFetcher() {
    this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
  }

  /** Package-private seam: inject an {@link HttpClient} for tests. */
  PmcBiocFetcher(HttpClient http) {
    _http = http;
  }

  /**
   * @return concatenated relevant passage text for the given PubMed id
   * @throws TextUnavailableException if the paper text cannot be resolved
   *         (non-2xx, non-JSON, malformed, or no relevant full-text passages)
   */
  public String fetch(String pubmedId) throws TextUnavailableException {
    HttpResponse<String> response;
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(BIOC_URL_BASE + pubmedId))
          .timeout(HTTP_TIMEOUT)
          .header("Accept", "application/json")
          .GET()
          .build();
      response = _http.send(request, HttpResponse.BodyHandlers.ofString());
    }
    catch (IOException e) {
      throw new TextUnavailableException(
          "could not reach PMC BioC for PMID " + pubmedId + ": " + e.getMessage());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TextUnavailableException("interrupted fetching PMID " + pubmedId);
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new TextUnavailableException(
          "PMID " + pubmedId + " is not available as PMC BioC JSON (HTTP "
              + response.statusCode() + ")");
    }

    String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
    if (!contentType.startsWith("application/json")) {
      throw new TextUnavailableException(
          "PMID " + pubmedId + " is not available as PMC BioC JSON (full text likely unavailable)");
    }

    JsonNode root;
    try {
      root = MAPPER.readTree(response.body());
    }
    catch (IOException e) {
      throw new TextUnavailableException(
          "PMID " + pubmedId + " returned malformed BioC JSON");
    }

    String text = parseBiocJson(root);
    if (text.isEmpty()) {
      throw new TextUnavailableException(
          "PMID " + pubmedId + " has no extractable full-text passages");
    }
    return text;
  }

  /**
   * Extract the relevant full-text from a parsed PMC BioC document (the response
   * is a top-level array of docs, each with {@code documents → passages}). Pure
   * port of Python {@code parse_pubmed_json}.
   *
   * @return concatenated non-empty {@code text} of passages whose
   *         {@code infons.section_type} is relevant, joined with newlines (may be
   *         empty if no relevant passages are present)
   */
  static String parseBiocJson(JsonNode root) {
    StringBuilder out = new StringBuilder();
    for (JsonNode doc : root) {
      for (JsonNode document : doc.path("documents")) {
        for (JsonNode passage : document.path("passages")) {
          String section = passage.path("infons").path("section_type").asText("");
          if (section.isEmpty() || !RELEVANT_SECTIONS.contains(section.toUpperCase())) {
            continue;
          }
          String text = passage.path("text").asText("");
          if (!text.isEmpty()) {
            if (out.length() > 0) {
              out.append('\n');
            }
            out.append(text);
          }
        }
      }
    }
    return out.toString();
  }

  /** Signals a terminal {@code text-unavailable} outcome (not cached). */
  public static class TextUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public TextUnavailableException(String reason) {
      super(reason);
    }
  }
}
