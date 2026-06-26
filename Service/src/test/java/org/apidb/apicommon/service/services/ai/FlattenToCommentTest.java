package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the stage-④ flatten of a {@code getGeneSummary} JSON object into a
 * plain-text-markdown comment: {@code headline = Headline} and a {@code content}
 * body of an "Executive summary:" section ({@code ShortSummary}), a "Details:"
 * section of {@code - } bullets each tagged with a {@code [n]} reference, an
 * "Evidence:" section pairing each {@code [n]} with its location and {@code - }
 * quote lines, then "Additional inferences" and "Aliases mentioned in paper"
 * sections. Plain text only — no HTML; the client renders newlines as
 * {@code <br />}.
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
  public void headlineIsTheHeadlineField() {
    JsonNode summary = json("{\"Headline\": \"Surface antigen essential for mosquito-stage transmission.\","
        + "\"ShortSummary\": \"Pfs25 is essential for transmission.\"}");
    assertEquals("Surface antigen essential for mosquito-stage transmission.",
        AiGenePublicationPipeline.flattenHeadline(summary));
  }

  @Test
  public void headlineIsEmptyWhenHeadlineMissing() {
    assertEquals("", AiGenePublicationPipeline.flattenHeadline(json("{}")));
  }

  @Test
  public void contentRendersExecutiveSummaryDetailsEvidenceInferencesAndAliases() {
    JsonNode summary = json("{"
        + "\"ShortSummary\": \"Pfs25 is a transmission-blocking vaccine target.\","
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
        "Executive summary:",
        "",
        "Pfs25 is a transmission-blocking vaccine target.",
        "",
        "Details:",
        "",
        "- Pfs25 is on the ookinete surface. [1]",
        "- Knockouts block transmission. [2]",
        "",
        "Evidence:",
        "",
        "[1] Figure 2",
        "- detected on ookinetes",
        "- strong signal",
        "",
        "[2] Results",
        "",
        "Additional inferences:",
        "",
        "- May be a vaccine target.",
        "",
        "Aliases mentioned in paper: Pfs25, P25");

    assertEquals(expected, AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void bulletIsNumberedButEvidenceStanzaOmittedWhenLocationAndQuotesBlank() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"\", \"supporting_quotes\": []}]}");
    assertEquals(String.join("\n", "Details:", "", "- A claim. [1]"),
        AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsInferencesSectionWhenEmpty() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}],"
        + "\"AdditionalInferences\": []}");
    assertEquals(String.join("\n", "Details:", "", "- A claim. [1]", "", "Evidence:", "", "[1] Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsAliasesLineWhenNoneMentioned() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}],"
        + "\"Aliases_in_paper\": []}");
    assertEquals(String.join("\n", "Details:", "", "- A claim. [1]", "", "Evidence:", "", "[1] Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary));
  }

  @Test
  public void contentOmitsExecutiveSummaryWhenShortSummaryMissing() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}]}");
    assertEquals(String.join("\n", "Details:", "", "- A claim. [1]", "", "Evidence:", "", "[1] Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary));
  }
}
