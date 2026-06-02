package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.junit.Test;

/**
 * Tests for the {@code fetching-article} stage (deliverable 2): the upload path
 * passes the FE-supplied text straight through; the pubmed path delegates to a
 * {@link PmcBiocFetcher} and translates a {@link TextUnavailableException} into a
 * terminal {@code text-unavailable} job state.
 */
public class AiGenePublicationPipelineTest {

  private static JobState jobState(String documentType, String pubmedId, String paperText) {
    AiGenePublicationRequest r = new AiGenePublicationRequest();
    r.geneId = "PF3D7_1133400";
    r.documentType = documentType;
    r.pubmedId = pubmedId;
    r.paperText = paperText;
    JobSubmission submission = new JobSubmission(
        r, "deadbeef", Collections.<String>emptyList(), "claude-sonnet-4", "1", "{}");
    return new JobState(submission, 7L);
  }

  /** A fetcher that returns canned text without any network access. */
  private static PmcBiocFetcher cannedFetcher(final String text) {
    return new PmcBiocFetcher() {
      @Override public String fetch(String pubmedId) { return text; }
    };
  }

  /** A fetcher that always signals the article text is unavailable. */
  private static PmcBiocFetcher unavailableFetcher(final String reason) {
    return new PmcBiocFetcher() {
      @Override public String fetch(String pubmedId) throws TextUnavailableException {
        throw new TextUnavailableException(reason);
      }
    };
  }

  @Test
  public void uploadPathUsesFrontEndSuppliedText() {
    JobState job = jobState("upload", null, "Gene PF3D7_1133400 is characterised here.");
    AiGenePublicationPipeline pipeline =
        new AiGenePublicationPipeline(job, null, unavailableFetcher("should-not-be-called"));

    pipeline.fetchArticle();

    assertEquals("Gene PF3D7_1133400 is characterised here.", pipeline.articleText());
    assertEquals(JobState.Stage.FETCHING_ARTICLE, job.getStage());
    assertEquals(JobStatus.RUNNING, job.getStatus());
  }

  @Test
  public void pubmedPathUsesFetchedText() {
    JobState job = jobState("pubmed", "12345678", null);
    AiGenePublicationPipeline pipeline =
        new AiGenePublicationPipeline(job, null, cannedFetcher("fetched body text"));

    pipeline.fetchArticle();

    assertEquals("fetched body text", pipeline.articleText());
    assertEquals(JobStatus.RUNNING, job.getStatus());
  }

  @Test
  public void pubmedTextUnavailableMarksTerminal() {
    JobState job = jobState("pubmed", "12345678", null);
    AiGenePublicationPipeline pipeline =
        new AiGenePublicationPipeline(job, null, unavailableFetcher("PMID not open-access"));

    pipeline.fetchArticle();

    assertEquals(JobStatus.TEXT_UNAVAILABLE, job.getStatus());
    assertNull("text-unavailable must not set article text", pipeline.articleText());
    TerminalResult result = (TerminalResult) job.getResult();
    assertEquals(JobStatus.TEXT_UNAVAILABLE, result.getStatus());
    assertEquals("PMID not open-access", result.getDetail());
  }
}
