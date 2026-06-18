package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.apidb.apicommon.service.services.ai.gene.GeneSynonymService;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Tests for the publish endpoint's pure mapping (deliverable 7): turning a
 * cached {@link CommentAiRun} row plus the user-submitted {@code headline} /
 * {@code content} into the {@link CommentRequest} (gene target + AI provenance)
 * that {@code createComment} persists. The {@code is_edited} flag is derived
 * here by comparing the submitted text to the run's AI original.
 */
public class AiGenePublicationPublishTest {

  private static CommentAiRun successRun() {
    return new CommentAiRun()
        .setJobId("deadbeef")
        .setGeneId("PF3D7_1133400")
        .setTerminalStatus("success")
        .setAiHeadline("Pfs25 matters.")
        .setAiContent("- A finding.");
  }

  @Test
  public void buildsGeneTargetAndProvenanceFromRunRow() {
    Date now = new Date(1_700_000_000_000L);
    CommentRequest req = AiGenePublicationCommentService.buildPublishComment(
        successRun(), "Pfs25 matters.", "- A finding.", now);

    assertEquals(GeneSynonymService.GENE_URL_SEGMENT, req.getTarget().getType());
    assertEquals("PF3D7_1133400", req.getTarget().getId());
    assertEquals("Pfs25 matters.", req.getHeadline());
    assertEquals("- A finding.", req.getContent());

    AiProvenance prov = req.getAiProvenance();
    assertNotNull("publish attaches AI provenance", prov);
    assertEquals("deadbeef", prov.getRunJobId());
    assertEquals(now, prov.getCreatedAt());
  }

  @Test
  public void isEditedFalseWhenTextMatchesAiOriginal() {
    CommentRequest req = AiGenePublicationCommentService.buildPublishComment(
        successRun(), "Pfs25 matters.", "- A finding.", new Date());
    assertFalse("unchanged text → not edited", req.getAiProvenance().isEdited());
  }

  @Test
  public void isEditedTrueWhenContentDiffers() {
    CommentRequest req = AiGenePublicationCommentService.buildPublishComment(
        successRun(), "Pfs25 matters.", "- An edited finding.", new Date());
    assertTrue("changed content → edited", req.getAiProvenance().isEdited());
  }

  @Test
  public void isEditedTrueWhenHeadlineDiffers() {
    CommentRequest req = AiGenePublicationCommentService.buildPublishComment(
        successRun(), "A new headline.", "- A finding.", new Date());
    assertTrue("changed headline → edited", req.getAiProvenance().isEdited());
  }

  @Test
  public void isEditedTrueWhenNoAiOriginal() {
    // gene-not-mentioned / mentioned-in-passing rows carry null ai_headline/ai_content;
    // anything the user writes is, by construction, an edit.
    CommentAiRun run = new CommentAiRun()
        .setJobId("cafe")
        .setGeneId("PF3D7_0100100")
        .setTerminalStatus("gene-not-mentioned");
    CommentRequest req = AiGenePublicationCommentService.buildPublishComment(
        run, "My own headline", "My own observations.", new Date());
    assertTrue("user-written body over a null AI original → edited",
        req.getAiProvenance().isEdited());
  }

  // --- source rendering -----------------------------------------------------

  @Test
  public void sourceJsonPubmedRunHasKindAndPmidOnly() {
    CommentAiRun run = new CommentAiRun().setSourceKind("pubmed").setPubmedId("39324028");
    JSONObject json = AiGenePublicationCommentService.sourceJson(run);

    assertEquals("pubmed", json.getString("kind"));
    assertEquals("39324028", json.getString("pubmed_id"));
    assertFalse("no upload fields on a pubmed source", json.has("pdf_content_sha256"));
    assertEquals("source carries only kind + pubmed_id", 2, json.length());
  }

  @Test
  public void sourceJsonUploadRunHasDigestAndOptionalProvenance() {
    CommentAiRun run = new CommentAiRun()
        .setSourceKind("upload")
        .setPdfContentSha256("abcd1234")
        .setExternalUrl("http://x/paper.pdf")
        .setExternalTitle("A Paper");
    JSONObject json = AiGenePublicationCommentService.sourceJson(run);

    assertEquals("upload", json.getString("kind"));
    assertEquals("abcd1234", json.getString("pdf_content_sha256"));
    assertEquals("http://x/paper.pdf", json.getString("external_url"));
    assertEquals("A Paper", json.getString("external_title"));
    assertFalse("no pubmed_id on an upload source", json.has("pubmed_id"));
  }

  @Test
  public void sourceJsonUploadOmitsNullUrlAndTitle() {
    CommentAiRun run = new CommentAiRun().setSourceKind("upload").setPdfContentSha256("abcd1234");
    JSONObject json = AiGenePublicationCommentService.sourceJson(run);

    assertEquals("upload", json.getString("kind"));
    assertEquals("abcd1234", json.getString("pdf_content_sha256"));
    assertFalse(json.has("external_url"));
    assertFalse(json.has("external_title"));
    assertEquals("source carries only kind + pdf_content_sha256", 2, json.length());
  }
}
