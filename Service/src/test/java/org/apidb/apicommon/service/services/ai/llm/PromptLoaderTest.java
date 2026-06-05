package org.apidb.apicommon.service.services.ai.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link PromptLoader}: classpath resource loading for the ported
 * {@code getGeneSummary} stage, blank-line splitting of user turns, and naive
 * {@code [PLACEHOLDER]} substitution (port of {@code get_prompt_and_replace}).
 */
public class PromptLoaderTest {

  private final PromptLoader loader = new PromptLoader();

  @Test
  public void systemPromptLoadsWithUnsubstitutedPlaceholders() {
    String system = loader.system("getGeneSummary");
    assertTrue(system.contains("ROLE: You are a scientist"));
    assertTrue("system prompt keeps the [JSON_SCHEMA] marker for substitution",
        system.contains("[JSON_SCHEMA]"));
    assertTrue("system prompt keeps the [N_QUOTES] marker", system.contains("[N_QUOTES]"));
  }

  @Test
  public void userPromptSplitsIntoTwoTurnsOnBlankLine() {
    List<String> turns = loader.userTurns("getGeneSummary");
    assertEquals(2, turns.size());
    // turn 1 carries the paper-text placeholder (and its own internal newline)
    assertTrue(turns.get(0).contains("Do not respond to this message."));
    assertTrue(turns.get(0).contains("[PAPER_TEXT]"));
    // turn 2 carries the gene placeholder and must NOT include the paper text
    assertTrue(turns.get(1).contains("[GENE]"));
    assertTrue(!turns.get(1).contains("[PAPER_TEXT]"));
  }

  @Test
  public void schemaLoadsAsRawJsonText() {
    String schema = loader.schema("getGeneSummary");
    assertTrue(schema.contains("only_in_passing"));
    assertTrue(schema.contains("\"additionalProperties\": false"));
  }

  @Test
  public void renderSubstitutesEveryOccurrenceOfAMarker() {
    assertEquals("X then X again",
        PromptLoader.render("[GENE] then [GENE] again", one("GENE", "X")));
  }

  @Test
  public void renderSubstitutesMultipleDistinctMarkers() {
    Map<String, String> r = new HashMap<>();
    r.put("GENE", "PF3D7_1133400");
    r.put("N_QUOTES", "2");
    assertEquals("gene PF3D7_1133400 with 2 quotes",
        PromptLoader.render("gene [GENE] with [N_QUOTES] quotes", r));
  }

  @Test
  public void renderLeavesUnknownMarkersUntouched() {
    assertEquals("keep [UNKNOWN] here",
        PromptLoader.render("keep [UNKNOWN] here", one("GENE", "X")));
  }

  private static Map<String, String> one(String k, String v) {
    Map<String, String> m = new HashMap<>();
    m.put(k, v);
    return m;
  }
}
