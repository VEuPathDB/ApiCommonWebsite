package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apidb.apicommon.model.comment.pojo.AiProvenanceView;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Locks the JSON serialization of {@link AiProvenanceView} to the client
 * {@code AiProvenance} contract: the {@code isEdited} key (not Jackson's default
 * {@code edited}), the {@code source} discriminated union, and NON_NULL omission
 * of the union arms that don't apply to the active {@code kind}.
 */
public class AiProvenanceViewTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode tree(Object o) {
    return MAPPER.valueToTree(o);
  }

  @Test
  public void pubmedSourceSerializesPmidOnly() {
    JsonNode json = tree(new AiProvenanceView(
        false, AiProvenanceView.Source.pubmed("12345"), "Headline", "Content"));

    assertFalse(json.get("isEdited").asBoolean());
    assertEquals("Headline", json.get("originalHeadline").asText());
    assertEquals("Content", json.get("originalContent").asText());

    JsonNode source = json.get("source");
    assertEquals("pubmed", source.get("kind").asText());
    assertEquals("12345", source.get("pubmedId").asText());
    // upload-only fields are absent on a pubmed source
    assertEquals("source carries only kind + pubmedId", 2, source.size());
  }

  @Test
  public void uploadSourceSerializesAllPresentFields() {
    JsonNode json = tree(new AiProvenanceView(
        true,
        AiProvenanceView.Source.upload("http://x/paper.pdf", "A Paper", "abcd1234"),
        "H", "C"));

    assertTrue(json.get("isEdited").asBoolean());

    JsonNode source = json.get("source");
    assertEquals("upload", source.get("kind").asText());
    assertEquals("http://x/paper.pdf", source.get("externalUrl").asText());
    assertEquals("A Paper", source.get("externalTitle").asText());
    assertEquals("abcd1234", source.get("pdfContentSha256").asText());
    assertFalse("no pubmedId on an upload source", source.has("pubmedId"));
  }

  @Test
  public void uploadSourceOmitsNullUrlAndTitleButKeepsDigest() {
    JsonNode json = tree(new AiProvenanceView(
        false, AiProvenanceView.Source.upload(null, null, "abcd1234"), "H", "C"));

    JsonNode source = json.get("source");
    assertEquals("upload", source.get("kind").asText());
    assertEquals("abcd1234", source.get("pdfContentSha256").asText());
    assertFalse(source.has("externalUrl"));
    assertFalse(source.has("externalTitle"));
    assertEquals("source carries only kind + pdfContentSha256", 2, source.size());
  }
}
