package org.apidb.apicommon.service.services.ai.article;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.UpstreamUnavailableException;
import org.junit.Test;

/**
 * Unit tests for {@link PmcBiocFetcher}: the pure BioC-JSON passage extraction,
 * ported from Python {@code parse_pubmed_json} (helpers.py) — keep passages whose
 * {@code infons.section_type} (case-insensitively) is one of
 * {FIG, TABLE, RESULTS, CONCL, DISCUSSION, SUPPL}, concatenate their non-empty
 * {@code text} fields with newlines, in document order — plus the {@code fetch}
 * failure taxonomy, which decides whether a failure is NCBI's problem or the
 * paper's.
 */
public class PmcBiocFetcherTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String s) {
    try {
      return MAPPER.readTree(s);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** The BioC payload is a top-level array of docs, each with documents → passages. */
  private static String bioc(String passagesJson) {
    return "[{\"documents\":[{\"passages\":[" + passagesJson + "]}]}]";
  }

  private static String passage(String sectionType, String text) {
    return "{\"infons\":{\"section_type\":\"" + sectionType + "\"},\"text\":\"" + text + "\"}";
  }

  @Test
  public void keepsOnlyRelevantSectionsInDocumentOrder() {
    String body = bioc(
        passage("RESULTS", "r1") + "," +
        passage("INTRO", "i1") + "," +
        passage("DISCUSS", "d1") + "," +
        passage("METHODS", "m1") + "," +
        passage("CONCL", "c1"));
    assertEquals("r1\nd1\nc1", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  @Test
  public void sectionTypeMatchIsCaseInsensitive() {
    String body = bioc(
        passage("results", "lower") + "," +
        passage("Table", "mixed") + "," +
        passage("SUPPL", "upper"));
    assertEquals("lower\nmixed\nupper", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  @Test
  public void skipsPassagesWithMissingOrEmptyText() {
    String body = "[{\"documents\":[{\"passages\":["
        + "{\"infons\":{\"section_type\":\"RESULTS\"}}," // no text field
        + "{\"infons\":{\"section_type\":\"RESULTS\"},\"text\":\"\"}," // empty text
        + passage("RESULTS", "kept")
        + "]}]}]";
    assertEquals("kept", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  /**
   * The real PMC BioC vocabulary uses {@code DISCUSS}; the Python source
   * filtered for {@code DISCUSSION} (matching nothing). The Java port corrects
   * this, so the legacy spelling must NOT be matched.
   */
  @Test
  public void legacyDiscussionSpellingIsNotMatched() {
    String body = bioc(
        passage("DISCUSSION", "wrong-token") + "," +
        passage("DISCUSS", "kept"));
    assertEquals("kept", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  @Test
  public void returnsEmptyWhenNoRelevantSections() {
    String body = bioc(
        passage("TITLE", "t") + "," +
        passage("ABSTRACT", "a") + "," +
        passage("INTRO", "i"));
    assertEquals("", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  @Test
  public void passageWithoutSectionTypeIsSkipped() {
    String body = "[{\"documents\":[{\"passages\":["
        + "{\"infons\":{},\"text\":\"no-section\"}," // missing section_type
        + passage("FIG", "kept")
        + "]}]}]";
    assertEquals("kept", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  @Test
  public void concatenatesAcrossMultipleDocuments() {
    String body = "["
        + "{\"documents\":[{\"passages\":[" + passage("RESULTS", "doc1") + "]}]},"
        + "{\"documents\":[{\"passages\":[" + passage("DISCUSS", "doc2") + "]}]}"
        + "]";
    assertEquals("doc1\ndoc2", PmcBiocFetcher.parseBiocJson(json(body)));
  }

  // --- fetch() failure taxonomy ---------------------------------------------
  //
  // The split that matters: an NCBI outage must NOT look like "this paper has no
  // full text", because the two lead the user to opposite actions (retry later
  // vs. give up on the article). Driven through the BiocHttpExchange seam, so
  // no network is touched.

  private static final String OK_BODY =
      "[{\"documents\":[{\"passages\":[{\"infons\":{\"section_type\":\"RESULTS\"},"
          + "\"text\":\"findings\"}]}]}]";

  /** A fetcher whose transport returns one canned response. */
  private static PmcBiocFetcher fetcherReturning(int status, String contentType, String body) {
    return new PmcBiocFetcher(uri -> new PmcBiocFetcher.BiocResponse(status, contentType, body));
  }

  /** A fetcher whose transport throws, as it does when NCBI is unreachable. */
  private static PmcBiocFetcher fetcherThrowing(IOException e) {
    return new PmcBiocFetcher(uri -> { throw e; });
  }

  private static void assertUpstreamUnavailable(PmcBiocFetcher fetcher) {
    try {
      fetcher.fetch("21533217");
      fail("expected UpstreamUnavailableException");
    }
    catch (UpstreamUnavailableException expected) {
      // correct: NCBI's problem, retryable
    }
    catch (TextUnavailableException e) {
      fail("upstream outage was misreported as text-unavailable: " + e.getMessage());
    }
  }

  private static void assertTextUnavailable(PmcBiocFetcher fetcher) {
    try {
      fetcher.fetch("21533217");
      fail("expected TextUnavailableException");
    }
    catch (TextUnavailableException expected) {
      // correct: the paper's problem, not retryable
    }
    catch (UpstreamUnavailableException e) {
      fail("paper-level failure was misreported as an upstream outage: " + e.getMessage());
    }
  }

  /** The exact failure observed on 2026-08-10: istio gateway with no healthy backend. */
  @Test
  public void http503IsAnUpstreamOutage() {
    assertUpstreamUnavailable(fetcherReturning(503, "text/plain", "no healthy upstream"));
  }

  @Test
  public void http500IsAnUpstreamOutage() {
    assertUpstreamUnavailable(fetcherReturning(500, "text/html", "<html>error</html>"));
  }

  @Test
  public void http429IsAnUpstreamOutage() {
    assertUpstreamUnavailable(fetcherReturning(429, "text/plain", "slow down"));
  }

  @Test
  public void unreachableHostIsAnUpstreamOutage() {
    assertUpstreamUnavailable(fetcherThrowing(new IOException("connection refused")));
  }

  @Test
  public void readTimeoutIsAnUpstreamOutage() {
    assertUpstreamUnavailable(fetcherThrowing(new HttpTimeoutException("request timed out")));
  }

  @Test
  public void http404IsAPaperLevelFailure() {
    assertTextUnavailable(fetcherReturning(404, "text/plain", "not found"));
  }

  /** How this endpoint reports a non-open-access paper: 200, but not JSON. */
  @Test
  public void nonJsonBodyIsAPaperLevelFailure() {
    assertTextUnavailable(fetcherReturning(200, "text/plain", "PMID is not open access"));
  }

  @Test
  public void malformedJsonIsAPaperLevelFailure() {
    assertTextUnavailable(fetcherReturning(200, "application/json", "{ not json"));
  }

  @Test
  public void noRelevantPassagesIsAPaperLevelFailure() {
    String onlyIntro =
        "[{\"documents\":[{\"passages\":[{\"infons\":{\"section_type\":\"INTRO\"},"
            + "\"text\":\"background\"}]}]}]";
    assertTextUnavailable(fetcherReturning(200, "application/json", onlyIntro));
  }

  @Test
  public void successReturnsConcatenatedRelevantText() throws Exception {
    assertEquals("findings",
        fetcherReturning(200, "application/json", OK_BODY).fetch("21533217"));
  }

  /** Charset parameters are normal on this endpoint and must not fail the type check. */
  @Test
  public void contentTypeWithCharsetIsAccepted() throws Exception {
    assertEquals("findings",
        fetcherReturning(200, "application/json; charset=UTF-8", OK_BODY).fetch("21533217"));
  }

  @Test
  public void requestTargetsTheConfiguredBiocEndpoint() throws Exception {
    URI[] requested = new URI[1];
    PmcBiocFetcher fetcher = new PmcBiocFetcher(uri -> {
      requested[0] = uri;
      return new PmcBiocFetcher.BiocResponse(200, "application/json", OK_BODY);
    });
    fetcher.fetch("21533217");
    assertEquals(URI.create(PmcBiocFetcher.BIOC_URL_BASE + "21533217"), requested[0]);
  }
}
