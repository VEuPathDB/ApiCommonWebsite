package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the stage-④ flatten of a {@code getGeneSummary} JSON object into a
 * plain-text-markdown comment: {@code headline = ShortSummary} and a
 * {@code content} body of {@code - } bullets (with indented {@code Evidence:}
 * and {@code >} quote lines), an optional "Additional inferences" section, and
 * an "Aliases mentioned in paper:" line. Plain text only — no HTML (ports the
 * structure of Python {@code build_extended_summary_html}).
 */
public class FlattenToCommentTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String s) {
    try {
      return MAPPER.readTree(s);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void headlineIsTheShortSummary() {
    JsonNode summary = json("{\"ShortSummary\": \"Pfs25 is essential for transmission.\"}");
    assertEquals("Pfs25 is essential for transmission.",
        AiGenePublicationPipeline.flattenHeadline(summary));
  }

  @Test
  public void headlineIsEmptyWhenShortSummaryMissing() {
    assertEquals("", AiGenePublicationPipeline.flattenHeadline(json("{}")));
  }

  @Test
  public void contentRendersBulletsEvidenceQuotesInferencesAndAliases() {
    JsonNode summary = json("{"
        + "\"ShortSummary\": \"x\","
        + "\"Aliases_in_paper\": [\"Pfs25\", \"P25\"],"
        + "\"GeneSummary\": ["
        + "  {\"bullet_point\": \"Pfs25 is on the ookinete surface.\","
        + "   \"evidence_location\": \"Figure 2\","
        + "   \"supporting_quotes\": [\"detected on ookinetes\", \"strong signal\"]},"
        + "  {\"bullet_point\": \"Knockouts block transmission.\","
        + "   \"evidence_location\": \"Results\","
        + "   \"supporting_quotes\": []}"
        + "],"
        + "\"AdditionalInferences\": [\"May be a vaccine target.\"]"
        + "}");

    String expected = String.join("\n",
        "- Pfs25 is on the ookinete surface.",
        "  Evidence: Figure 2",
        "  > detected on ookinetes",
        "  > strong signal",
        "- Knockouts block transmission.",
        "  Evidence: Results",
        "",
        "Additional inferences:",
        "- May be a vaccine target.",
        "",
        "Aliases mentioned in paper: Pfs25, P25");

    assertEquals(expected, AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsEvidenceLineWhenLocationBlank() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"\", \"supporting_quotes\": []}]}");
    assertEquals("- A claim.", AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsInferencesSectionWhenEmpty() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}],"
        + "\"AdditionalInferences\": []}");
    assertEquals(String.join("\n", "- A claim.", "  Evidence: Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsAliasesLineWhenNoneMentioned() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}],"
        + "\"Aliases_in_paper\": []}");
    assertEquals(String.join("\n", "- A claim.", "  Evidence: Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary));
  }
}
