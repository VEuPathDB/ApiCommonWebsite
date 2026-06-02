package org.apidb.apicommon.service.services.ai.article;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

/**
 * Unit tests for the pure BioC-JSON passage extraction, ported from Python
 * {@code parse_pubmed_json} (helpers.py): keep passages whose
 * {@code infons.section_type} (case-insensitively) is one of
 * {FIG, TABLE, RESULTS, CONCL, DISCUSSION, SUPPL}, concatenate their non-empty
 * {@code text} fields with newlines, in document order.
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
}
