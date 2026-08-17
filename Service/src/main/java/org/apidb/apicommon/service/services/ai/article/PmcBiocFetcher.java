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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stage ① article-text resolution for the PubMed path: fetches the PMC BioC
 * JSON document for a PubMed id, keeps passages whose
 * {@code infons.section_type} is one of {FIG, TABLE, RESULTS, CONCL,
 * DISCUSSION, SUPPL}, and concatenates their {@code text}.
 *
 * <p>Failures are split by <em>who owns the problem</em>, because the two need
 * very different things said to the user:
 *
 * <ul>
 *   <li>{@link UpstreamUnavailableException} — NCBI is down or unreachable
 *       (transport failure, HTTP 5xx, HTTP 429). Nothing is wrong with the
 *       article or the request; the same submission may well succeed later.
 *   <li>{@link TextUnavailableException} — NCBI answered, but this particular
 *       paper has no usable open-access full text (other non-2xx, a non-JSON
 *       body, malformed JSON, or no relevant passages). Retrying will not help;
 *       uploading the PDF will.
 * </ul>
 *
 * <p>Neither outcome is persisted to the cache, so retries always re-run the
 * fetch. The upload path has no fetch at all: the front-end-supplied
 * {@code paper_text} is used directly, which is why "upload the PDF instead"
 * remains a working escape hatch even during a total NCBI outage.
 *
 * <p>Ported from Python {@code get_pubmed_json} / {@code parse_pubmed_json}
 * (PubGene_back_end/helpers.py).
 */
public class PmcBiocFetcher {

  private static final Logger LOG = Logger.getLogger(PmcBiocFetcher.class);

  /**
   * The PMC Open Access BioC endpoint. Docs:
   * <a href="https://www.ncbi.nlm.nih.gov/research/bionlp/APIs/BioC-PMC/">BioC API for PMC OA</a>.
   *
   * <p><b>Alternative source, for future reference.</b> NCBI also serves the same
   * BioC content through PubTator3:
   * {@code https://www.ncbi.nlm.nih.gov/research/pubtator3-api/publications/export/biocjson?pmids={pmid}&full=true}.
   * On 2026-08-10 the whole {@code /research/bionlp/RESTful/*} tree was returning
   * {@code 503 no healthy upstream} (its istio gateway had no healthy backend)
   * while the PubTator3 endpoint served this same article normally — so it is a
   * viable fallback if these outages recur.
   *
   * <p>Its envelope differs and {@link #parseBiocJson} would need to accommodate
   * it: PubTator3 returns an <em>object</em> {@code {"PubTator3": [ … ]}} whose
   * docs carry {@code passages} directly, whereas this endpoint returns a
   * top-level <em>array</em> of docs whose passages sit under {@code documents}.
   * The {@code infons.section_type} vocabulary is identical.
   */
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

  /**
   * How much of an upstream error body to log. NCBI's outage responses are short
   * and diagnostic ({@code no healthy upstream}); this is enough to identify the
   * failure mode without dumping an HTML error page into the log.
   */
  private static final int BODY_LOG_CHARS = 200;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final BiocHttpExchange _http;

  public PmcBiocFetcher() {
    this(defaultExchange());
  }

  /** Package-private seam: inject a fake exchange for tests (no network). */
  PmcBiocFetcher(BiocHttpExchange http) {
    _http = http;
  }

  /** The real transport; the only part of this class that knows about {@link HttpClient}. */
  private static BiocHttpExchange defaultExchange() {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    return uri -> {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(uri)
          .timeout(HTTP_TIMEOUT)
          .header("Accept", "application/json")
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return new BiocResponse(
          response.statusCode(),
          response.headers().firstValue("Content-Type").orElse(""),
          response.body());
    };
  }

  /**
   * @return concatenated relevant passage text for the given PubMed id
   * @throws UpstreamUnavailableException if NCBI could not be reached or returned
   *         a transient server error — the request may succeed if retried later
   * @throws TextUnavailableException if NCBI answered but this paper's full text
   *         cannot be resolved (non-JSON, malformed, or no relevant passages)
   */
  public String fetch(String pubmedId)
      throws TextUnavailableException, UpstreamUnavailableException {
    String url = BIOC_URL_BASE + pubmedId;
    long start = System.currentTimeMillis();
    BiocResponse response;

    try {
      response = _http.get(URI.create(url));
    }
    catch (IOException e) {
      // Transport failure (connect refused, DNS, read timeout) — never the paper.
      LOG.error("PMC BioC unreachable for PMID " + pubmedId + " [" + url + "] after "
          + elapsedMs(start) + " ms; treating as upstream outage", e);
      throw new UpstreamUnavailableException(
          "could not reach the PMC BioC service for PMID " + pubmedId + ": " + e.getMessage());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Interrupted while fetching PMC BioC for PMID " + pubmedId + " [" + url + "]");
      throw new UpstreamUnavailableException("interrupted fetching PMID " + pubmedId);
    }

    long elapsed = elapsedMs(start);
    int status = response.statusCode();

    // 5xx / 429 are NCBI telling us it cannot serve right now. Logged at ERROR
    // with the body snippet: during the 2026-08-10 outage that snippet read
    // "no healthy upstream", which is what identified it as their problem.
    if (status >= 500 || status == 429) {
      LOG.error("PMC BioC returned HTTP " + status + " for PMID " + pubmedId + " [" + url
          + "] after " + elapsed + " ms; treating as upstream outage. Body: "
          + snippet(response.body()));
      throw new UpstreamUnavailableException(
          "the PMC BioC service returned HTTP " + status + " for PMID " + pubmedId);
    }

    if (status < 200 || status >= 300) {
      LOG.warn("PMC BioC returned HTTP " + status + " for PMID " + pubmedId + " [" + url
          + "] after " + elapsed + " ms; treating as no open-access full text");
      throw new TextUnavailableException(
          "PMID " + pubmedId + " is not available as PMC BioC JSON (HTTP " + status + ")");
    }

    // A 200 that isn't JSON is how this endpoint reports a non-open-access paper.
    String contentType = response.contentType().toLowerCase();
    if (!contentType.startsWith("application/json")) {
      LOG.warn("PMC BioC returned non-JSON content-type '" + response.contentType()
          + "' for PMID " + pubmedId + " after " + elapsed
          + " ms; paper is likely not open access");
      throw new TextUnavailableException(
          "PMID " + pubmedId + " is not available as PMC BioC JSON (full text likely unavailable)");
    }

    JsonNode root;
    try {
      root = MAPPER.readTree(response.body());
    }
    catch (IOException e) {
      LOG.warn("PMC BioC returned malformed JSON for PMID " + pubmedId + " after " + elapsed
          + " ms: " + e.getMessage() + ". Body: " + snippet(response.body()));
      throw new TextUnavailableException(
          "PMID " + pubmedId + " returned malformed BioC JSON");
    }

    String text = parseBiocJson(root);
    if (text.isEmpty()) {
      LOG.warn("PMC BioC document for PMID " + pubmedId + " had no passages in "
          + RELEVANT_SECTIONS + " after " + elapsed + " ms; nothing to summarise");
      throw new TextUnavailableException(
          "PMID " + pubmedId + " has no extractable full-text passages");
    }

    LOG.info("PMC BioC fetch succeeded for PMID " + pubmedId + " in " + elapsed + " ms; extracted "
        + text.length() + " chars of relevant full text");
    return text;
  }

  private static long elapsedMs(long start) {
    return System.currentTimeMillis() - start;
  }

  /** First {@link #BODY_LOG_CHARS} characters of a response body, for diagnostics. */
  private static String snippet(String body) {
    if (body == null) {
      return "(no body)";
    }
    String trimmed = body.strip();
    return trimmed.length() <= BODY_LOG_CHARS
        ? trimmed
        : trimmed.substring(0, BODY_LOG_CHARS) + "… (truncated)";
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

  /**
   * The three things {@link #fetch} needs from an HTTP response. Keeping this
   * narrow (rather than passing {@link HttpResponse} around) is what lets tests
   * drive every branch of the failure taxonomy from a lambda.
   */
  record BiocResponse(int statusCode, String contentType, String body) {}

  /** Test seam for the transport; the production implementation is {@link #defaultExchange()}. */
  @FunctionalInterface
  interface BiocHttpExchange {
    BiocResponse get(URI uri) throws IOException, InterruptedException;
  }

  /**
   * Signals a terminal {@code text-unavailable} outcome (not cached): NCBI
   * answered, but this paper has no usable open-access full text.
   */
  public static class TextUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public TextUnavailableException(String reason) {
      super(reason);
    }
  }

  /**
   * Signals a terminal {@code upstream-unavailable} outcome (not cached): NCBI
   * itself could not serve the request, so the same submission may succeed later.
   */
  public static class UpstreamUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public UpstreamUnavailableException(String reason) {
      super(reason);
    }
  }
}
