package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.model.comment.pojo.JobStatus;
import org.junit.Test;

/**
 * Tests for {@link AiProvenance#fromRun}, the shared rule that derives a
 * per-comment provenance row (run id + {@code is_edited} + {@code created_at})
 * from a cached {@link CommentAiRun} plus the submitted text. Used both by the
 * AI publish endpoint and by the carry-forward when a published AI comment is
 * edited through the normal comment form. Provenance rows only ever originate
 * from {@code success} runs, so the AI original headline/content is non-null.
 */
public class AiProvenanceFromRunTest {

  private static CommentAiRun successRun() {
    return new CommentAiRun()
        .setJobId("deadbeef")
        .setGeneId("PF3D7_1133400")
        .setTerminalStatus(JobStatus.SUCCESS)
        .setAiHeadline("Pfs25 matters.")
        .setAiContent("- A finding.");
  }

  @Test
  public void carriesRunIdAndCreatedAt() {
    Date now = new Date(1_700_000_000_000L);
    AiProvenance prov = AiProvenance.fromRun(successRun(), "Pfs25 matters.", "- A finding.", now);
    assertEquals("deadbeef", prov.getRunJobId());
    assertEquals(now, prov.getCreatedAt());
  }

  @Test
  public void notEditedWhenTextMatchesAiOriginal() {
    AiProvenance prov = AiProvenance.fromRun(
        successRun(), "Pfs25 matters.", "- A finding.", new Date());
    assertFalse("unchanged text → not edited", prov.isEdited());
  }

  @Test
  public void editedWhenContentDiffers() {
    AiProvenance prov = AiProvenance.fromRun(
        successRun(), "Pfs25 matters.", "- An edited finding.", new Date());
    assertTrue("changed content → edited", prov.isEdited());
  }

  @Test
  public void editedWhenHeadlineDiffers() {
    AiProvenance prov = AiProvenance.fromRun(
        successRun(), "A new headline.", "- A finding.", new Date());
    assertTrue("changed headline → edited", prov.isEdited());
  }
}
