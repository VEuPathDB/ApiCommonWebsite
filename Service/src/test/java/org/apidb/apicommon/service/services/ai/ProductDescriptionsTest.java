package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the compulsory {@code generatePDs} stage's pure helpers: the
 * {@code collect_bullets} port that feeds the summary bullets into the PD prompt,
 * and {@code flattenProductDescriptions} which renders the
 * "AI-suggested product description(s):" section (heading pluralised by count)
 * placed before the "Evidence:" section in the flattened comment content.
 */
public class ProductDescriptionsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String s) {
    try {
      return MAPPER.readTree(s);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // --- collectBullets (port of Python collect_bullets) ----------------------

  @Test
  public void collectBulletsReturnsBulletPointsInOrder() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"First claim.\"},"
        + "{\"bullet_point\": \"Second claim.\"}]}");
    assertEquals(Arrays.asList("First claim.", "Second claim."),
        AiGenePublicationPipeline.collectBullets(summary));
  }

  @Test
  public void collectBulletsSkipsItemsWithoutBulletPoint() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"Kept.\"},"
        + "{\"evidence_location\": \"Fig 1\"}]}");
    assertEquals(Collections.singletonList("Kept."),
        AiGenePublicationPipeline.collectBullets(summary));
  }

  @Test
  public void collectBulletsEmptyWhenNoGeneSummary() {
    assertEquals(Collections.emptyList(),
        AiGenePublicationPipeline.collectBullets(json("{}")));
  }

  // --- flattenProductDescriptions -------------------------------------------

  @Test
  public void rendersMultiplePdsWithPluralHeadingAndIndentedReason() {
    JsonNode pds = json("{\"PDs\": ["
        + "{\"description\": \"Fructose 1,6-bisphosphatase FBP1\","
        + " \"evidence_code\": \"IDA\", \"code_reason\": \"Direct enzyme assay in Fig 3.\"},"
        + "{\"description\": \"hypothetical protein, conserved\","
        + " \"evidence_code\": \"ISO\", \"code_reason\": \"Orthology only.\"}]}");

    String expected = String.join("\n",
        "AI-suggested product descriptions:",
        "",
        "- Fructose 1,6-bisphosphatase FBP1 [IDA]",
        "  Direct enzyme assay in Fig 3.",
        "- hypothetical protein, conserved [ISO]",
        "  Orthology only.");

    assertEquals(expected, AiGenePublicationPipeline.flattenProductDescriptions(pds));
  }

  @Test
  public void rendersSinglePdWithSingularHeading() {
    JsonNode pds = json("{\"PDs\": ["
        + "{\"description\": \"phosphatase PfPP1\", \"evidence_code\": \"IMP\","
        + " \"code_reason\": \"Knockout phenotype.\"}]}");

    String expected = String.join("\n",
        "AI-suggested product description:",
        "",
        "- phosphatase PfPP1 [IMP]",
        "  Knockout phenotype.");

    assertEquals(expected, AiGenePublicationPipeline.flattenProductDescriptions(pds));
  }

  @Test
  public void omitsReasonLineWhenCodeReasonBlank() {
    JsonNode pds = json("{\"PDs\": ["
        + "{\"description\": \"hypothetical protein\", \"evidence_code\": \"ISM\","
        + " \"code_reason\": \"\"}]}");

    String expected = String.join("\n",
        "AI-suggested product description:",
        "",
        "- hypothetical protein [ISM]");

    assertEquals(expected, AiGenePublicationPipeline.flattenProductDescriptions(pds));
  }

  @Test
  public void emptyWhenNoPds() {
    assertEquals("", AiGenePublicationPipeline.flattenProductDescriptions(json("{\"PDs\": []}")));
  }

  @Test
  public void emptyWhenPdsKeyAbsent() {
    assertEquals("", AiGenePublicationPipeline.flattenProductDescriptions(json("{}")));
  }

  // --- placement inside flattenContent (before the Evidence section) ---------

  @Test
  public void pdSectionSitsBetweenDetailsAndEvidence() {
    JsonNode summary = json("{"
        + "\"ShortSummary\": \"FBP1 catalyses gluconeogenesis.\","
        + "\"Aliases_in_paper\": [\"FBP1\"],"
        + "\"GeneSummary\": ["
        + "  {\"bullet_point\": \"FBP1 is a phosphatase.\","
        + "   \"evidence_location\": \"Figure 3\","
        + "   \"supporting_quotes\": [\"assayed FBP activity\"]}"
        + "],"
        + "\"AdditionalInferences\": [\"Possible drug target.\"]"
        + "}");
    JsonNode pds = json("{\"PDs\": ["
        + "{\"description\": \"Fructose 1,6-bisphosphatase FBP1\","
        + " \"evidence_code\": \"IDA\", \"code_reason\": \"Direct enzyme assay.\"}]}");

    String expected = String.join("\n",
        "Executive summary:",
        "",
        "FBP1 catalyses gluconeogenesis.",
        "",
        "Details:",
        "",
        "- FBP1 is a phosphatase. [1]",
        "",
        "AI-suggested product description:",
        "",
        "- Fructose 1,6-bisphosphatase FBP1 [IDA]",
        "  Direct enzyme assay.",
        "",
        "Evidence:",
        "",
        "[1] Figure 3",
        "- assayed FBP activity",
        "",
        "Additional inferences:",
        "",
        "- Possible drug target.",
        "",
        "Aliases mentioned in paper: FBP1");

    assertEquals(expected, AiGenePublicationPipeline.flattenContent(summary, pds));
  }

  @Test
  public void pdSectionOmittedWhenNoPdsButRestUnchanged() {
    JsonNode summary = json("{\"GeneSummary\": ["
        + "{\"bullet_point\": \"A claim.\", \"evidence_location\": \"Fig 1\", \"supporting_quotes\": []}]}");
    assertEquals(String.join("\n", "Details:", "", "- A claim. [1]", "", "Evidence:", "", "[1] Fig 1"),
        AiGenePublicationPipeline.flattenContent(summary, json("{\"PDs\": []}")));
  }
}
