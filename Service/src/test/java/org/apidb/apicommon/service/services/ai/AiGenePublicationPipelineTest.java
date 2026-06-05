package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.apidb.apicommon.service.services.ai.gene.GeneMentionScanner;
import org.apidb.apicommon.service.services.ai.llm.JsonPromptClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the {@code fetching-article} stage (deliverable 2): the upload path
 * passes the FE-supplied text straight through; the pubmed path delegates to a
 * {@link PmcBiocFetcher} and translates a {@link TextUnavailableException} into a
 * terminal {@code text-unavailable} job state.
 */
public class AiGenePublicationPipelineTest {

  private static JobState jobState(String documentType, String pubmedId, String paperText) {
    return jobState(documentType, pubmedId, paperText, Collections.<String>emptyList());
  }

  private static JobState jobState(String documentType, String pubmedId, String paperText,
      List<String> synonyms) {
    AiGenePublicationRequest r = new AiGenePublicationRequest();
    r.geneId = "PF3D7_1133400";
    r.documentType = documentType;
    r.pubmedId = pubmedId;
    r.paperText = paperText;
    JobSubmission submission = new JobSubmission(
        r, "deadbeef", synonyms, "claude-sonnet-4", "1", "{}");
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

  // --- scanning-gene-mentions stage (deliverable 3) -------------------------

  /** Run the upload-path fetch (which just passes the text through) then scan. */
  private static AiGenePublicationPipeline scannedUpload(String paperText, List<String> synonyms) {
    JobState job = jobState("upload", null, paperText, synonyms);
    AiGenePublicationPipeline pipeline = new AiGenePublicationPipeline(job, null);
    pipeline.fetchArticle();
    pipeline.scanGeneMentions();
    return pipeline;
  }

  @Test
  public void scanGeneMentionsStaysRunningAndRecordsNamesWhenGeneFound() {
    AiGenePublicationPipeline pipeline = scannedUpload(
        "The gene PF3D7_1133400 (Pfs25) is characterised in detail here.",
        Arrays.asList("Pfs25", "P25"));
    JobState job = pipeline.job();

    assertEquals(JobStatus.RUNNING, job.getStatus());
    assertEquals(JobState.Stage.SCANNING_GENE_MENTIONS, job.getStage());
    // gene id first, then the mentioned alias; the unmentioned alias is dropped
    assertEquals(Arrays.asList("PF3D7_1133400", "Pfs25"), pipeline.namesMentioned());
  }

  @Test
  public void scanGeneMentionsMarksGeneNotMentionedWhenAbsent() {
    AiGenePublicationPipeline pipeline = scannedUpload(
        "This paper is about something else entirely.",
        Arrays.asList("Pfs25", "P25"));
    JobState job = pipeline.job();

    assertEquals(JobStatus.GENE_NOT_MENTIONED, job.getStatus());
    TerminalResult result = (TerminalResult) job.getResult();
    assertEquals(JobStatus.GENE_NOT_MENTIONED, result.getStatus());

    // synonyms_checked lists the gene id plus every alias we looked for
    JSONObject json = result.toJson("deadbeef");
    assertEquals("gene-not-mentioned", json.getString("type"));
    assertEquals("deadbeef", json.getString("job_id"));
    JSONArray checked = json.getJSONArray("synonyms_checked");
    assertEquals(Arrays.asList("PF3D7_1133400", "Pfs25", "P25"), checked.toList());
  }

  // --- geneForPrompt (port of gene_for_prompt) ------------------------------

  @Test
  public void geneForPromptReturnsTheSoleNameUnchanged() {
    assertEquals("Nd6", AiGenePublicationPipeline.geneForPrompt("Nd6", Collections.singletonList("Nd6")));
  }

  @Test
  public void geneForPromptListsGeneFirstWhenGeneIsAmongNames() {
    assertEquals("Nd6 (also known as a or b)",
        AiGenePublicationPipeline.geneForPrompt("Nd6", Arrays.asList("Nd6", "a", "b")));
  }

  @Test
  public void geneForPromptUsesFirstMentionWhenGeneIdNotAmongNames() {
    assertEquals("a (also known as b or c)",
        AiGenePublicationPipeline.geneForPrompt("GENE", Arrays.asList("a", "b", "c")));
  }

  // --- generating-summary stage (deliverable 4) -----------------------------

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String s) {
    try {
      return MAPPER.readTree(s);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Build a pipeline through the scan stage (gene found) with an injected summary client. */
  private static AiGenePublicationPipeline summarised(String paperText, List<String> synonyms,
      JsonPromptClient client) {
    JobState job = jobState("upload", null, paperText, synonyms);
    AiGenePublicationPipeline pipeline = new AiGenePublicationPipeline(
        job, null, new PmcBiocFetcher(), new GeneMentionScanner(), client);
    pipeline.fetchArticle();
    pipeline.scanGeneMentions();
    return pipeline;
  }

  @Test
  public void generateSummaryStoresParsedSummaryAndStaysRunning() throws Exception {
    JsonPromptClient client = (stage, repl) -> json("{\"only_in_passing\": false, \"ShortSummary\": \"Key finding\"}");
    AiGenePublicationPipeline pipeline = summarised(
        "The gene PF3D7_1133400 is characterised here.", Collections.<String>emptyList(), client);

    pipeline.generateSummary();

    assertEquals(JobStatus.RUNNING, pipeline.job().getStatus());
    assertEquals(JobState.Stage.GENERATING_SUMMARY, pipeline.job().getStage());
    assertNotNull(pipeline.summaryJson());
    assertEquals("Key finding", pipeline.summaryJson().get("ShortSummary").asText());
  }

  @Test
  public void generateSummaryShortCircuitsToMentionedInPassing() throws Exception {
    JsonPromptClient client = (stage, repl) -> json("{\"only_in_passing\": true}");
    AiGenePublicationPipeline pipeline = summarised(
        "PF3D7_1133400 is named once.", Arrays.asList("Pfs25"), client);

    pipeline.generateSummary();

    JobState job = pipeline.job();
    assertEquals(JobStatus.MENTIONED_IN_PASSING, job.getStatus());
    TerminalResult result = (TerminalResult) job.getResult();
    JSONObject rendered = result.toJson("deadbeef");
    assertEquals("mentioned-in-passing", rendered.getString("type"));
    assertTrue("mentioned-in-passing carries synonyms_checked", rendered.has("synonyms_checked"));
  }

  @Test
  public void generateSummaryRendersGenePromptAndPaperTextIntoTheRequest() throws Exception {
    AtomicReference<String> stageRef = new AtomicReference<>();
    AtomicReference<Map<String, String>> replRef = new AtomicReference<>();
    JsonPromptClient client = (stage, repl) -> {
      stageRef.set(stage);
      replRef.set(repl);
      return json("{\"only_in_passing\": false}");
    };
    AiGenePublicationPipeline pipeline = summarised(
        "Studies of PF3D7_1133400 (Pfs25) in detail.", Arrays.asList("Pfs25"), client);

    pipeline.generateSummary();

    assertEquals("getGeneSummary", stageRef.get());
    Map<String, String> repl = replRef.get();
    assertEquals("2", repl.get("N_QUOTES"));
    assertEquals("PF3D7_1133400 (also known as Pfs25)", repl.get("GENE"));
    assertEquals("Studies of PF3D7_1133400 (Pfs25) in detail.", repl.get("PAPER_TEXT"));
  }

  // --- flatten-to-comment stage wiring (deliverable 5) ----------------------

  @Test
  public void flattenToCommentSetsHeadlineAndContentFromSummary() throws Exception {
    JsonNode summary = json("{"
        + "\"ShortSummary\": \"Pfs25 matters.\","
        + "\"GeneSummary\": [{\"bullet_point\": \"A finding.\","
        + "  \"evidence_location\": \"Fig 1\", \"supporting_quotes\": [\"q\"]}]}");
    JsonPromptClient client = (stage, repl) -> summary;
    AiGenePublicationPipeline pipeline = summarised(
        "The gene PF3D7_1133400 is studied.", Collections.<String>emptyList(), client);

    pipeline.generateSummary();
    pipeline.flattenToComment();

    assertEquals("Pfs25 matters.", pipeline.aiHeadline());
    assertEquals(String.join("\n", "- A finding.", "  Evidence: Fig 1", "  > q"),
        pipeline.aiContent());
  }
}
