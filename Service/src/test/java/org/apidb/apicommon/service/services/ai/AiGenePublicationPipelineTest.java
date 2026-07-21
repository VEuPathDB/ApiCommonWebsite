package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.model.comment.pojo.JobStatus;
import org.apidb.apicommon.model.comment.pojo.SourceKind;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.apidb.apicommon.service.services.ai.gene.GeneMentionScanner;
import org.apidb.apicommon.service.services.ai.llm.JsonPromptClient;
import org.gusdb.wdk.model.WdkModelException;
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
    r.documentType = SourceKind.fromWire(documentType);
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

  // --- generate-product-descriptions stage (compulsory) ---------------------

  @Test
  public void generateProductDescriptionsStoresPdsAndStaysRunning() throws Exception {
    JsonPromptClient client = (stage, repl) -> stage.equals("generatePDs")
        ? json("{\"PDs\": [{\"description\": \"phosphatase PfPP1\","
            + " \"evidence_code\": \"IMP\", \"code_reason\": \"Knockout phenotype.\"}]}")
        : json("{\"only_in_passing\": false, \"GeneSummary\": [{\"bullet_point\": \"PfPP1 is a phosphatase.\"}]}");
    AiGenePublicationPipeline pipeline = summarised(
        "PfPP1 characterised in PF3D7_1133400.", Collections.<String>emptyList(), client);

    pipeline.generateSummary();
    pipeline.generateProductDescriptions();

    assertEquals(JobStatus.RUNNING, pipeline.job().getStatus());
    assertEquals(JobState.Stage.GENERATING_PDS, pipeline.job().getStage());
    assertNotNull(pipeline.pdsJson());
    assertEquals("phosphatase PfPP1",
        pipeline.pdsJson().get("PDs").get(0).get("description").asText());
  }

  @Test
  public void generateProductDescriptionsRendersBulletsGeneAndCountIntoTheRequest() throws Exception {
    AtomicReference<String> stageRef = new AtomicReference<>();
    AtomicReference<Map<String, String>> replRef = new AtomicReference<>();
    JsonPromptClient client = (stage, repl) -> {
      if (stage.equals("generatePDs")) {
        stageRef.set(stage);
        replRef.set(repl);
        return json("{\"PDs\": []}");
      }
      return json("{\"only_in_passing\": false, \"GeneSummary\": ["
          + "{\"bullet_point\": \"First finding.\"}, {\"bullet_point\": \"Second finding.\"}]}");
    };
    AiGenePublicationPipeline pipeline = summarised(
        "Studies of PF3D7_1133400 (Pfs25).", Arrays.asList("Pfs25"), client);

    pipeline.generateSummary();
    pipeline.generateProductDescriptions();

    assertEquals("generatePDs", stageRef.get());
    Map<String, String> repl = replRef.get();
    assertEquals("3", repl.get("N_PDs"));
    assertEquals("PF3D7_1133400 (also known as Pfs25)", repl.get("GENE"));
    assertEquals("First finding.\nSecond finding.", repl.get("SUMMARY"));
  }

  // --- flatten-to-comment stage wiring (deliverable 5) ----------------------

  @Test
  public void flattenToCommentSetsHeadlineAndContentFromSummary() throws Exception {
    JsonNode summary = json("{"
        + "\"Headline\": \"A druggable surface antigen.\","
        + "\"ShortSummary\": \"Pfs25 matters.\","
        + "\"GeneSummary\": [{\"bullet_point\": \"A finding.\","
        + "  \"evidence_location\": \"Fig 1\", \"supporting_quotes\": [\"q\"]}]}");
    JsonPromptClient client = (stage, repl) -> summary;
    AiGenePublicationPipeline pipeline = summarised(
        "The gene PF3D7_1133400 is studied.", Collections.<String>emptyList(), client);

    pipeline.generateSummary();
    pipeline.flattenToComment();

    assertEquals("A druggable surface antigen.", pipeline.aiHeadline());
    assertEquals(String.join("\n",
        "Executive summary:", "", "Pfs25 matters.", "",
        "Details:", "", "- A finding. [1]", "",
        "Evidence:", "", "[1] Fig 1", "- q"),
        pipeline.aiContent());
  }

  // --- persisting stage (deliverable 6) -------------------------------------

  /** Capture-only store: records the row handed to persist, never touches a DB. */
  private static final class CapturingStore implements AiGenePublicationPipeline.AiRunStore {
    CommentAiRun captured;
    int calls;
    @Override public void persist(CommentAiRun run) { captured = run; calls++; }
  }

  /** Build a fully runnable pipeline with injected collaborators (no network, no DB). */
  private static AiGenePublicationPipeline runnable(String documentType, String pubmedId,
      String paperText, List<String> synonyms, PmcBiocFetcher fetcher,
      JsonPromptClient client, AiGenePublicationPipeline.AiRunStore store) {
    JobState job = jobState(documentType, pubmedId, paperText, synonyms);
    return new AiGenePublicationPipeline(
        job, null, fetcher, new GeneMentionScanner(), client, store);
  }

  @Test
  public void runSuccessPersistsRunRowAndPublishesAiOutput() throws Exception {
    JsonPromptClient client = (stage, repl) -> json("{"
        + "\"only_in_passing\": false,"
        + "\"Headline\": \"A druggable surface antigen.\","
        + "\"ShortSummary\": \"Pfs25 matters.\","
        + "\"GeneSummary\": [{\"bullet_point\": \"A finding.\","
        + "  \"evidence_location\": \"Fig 1\", \"supporting_quotes\": [\"q\"]}]}");
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "The gene PF3D7_1133400 is characterised here.", Arrays.asList("Pfs25"),
        new PmcBiocFetcher(), client, store);

    pipeline.run();

    JobState job = pipeline.job();
    assertEquals(JobStatus.SUCCESS, job.getStatus());

    assertEquals("the success path writes exactly one cache row", 1, store.calls);
    CommentAiRun row = store.captured;
    assertNotNull(row);
    assertEquals("deadbeef", row.getJobId());
    assertEquals(JobStatus.SUCCESS, row.getTerminalStatus());
    assertEquals(SourceKind.UPLOAD, row.getSource().kind());
    assertEquals("claude-sonnet-4", row.getModelName());
    assertEquals("1", row.getPromptVersion());
    assertEquals("{}", row.getOptionsJson());
    assertEquals(Arrays.asList("Pfs25"), row.getSynonymsUsed());
    assertEquals("A druggable surface antigen.", row.getAiHeadline());
    assertNotNull(row.getAiContent());
    assertFalse(row.isOnlyMentionedInPassing());
    assertNotNull(row.getCompletedAt());

    TerminalResult result = (TerminalResult) job.getResult();
    JSONObject rendered = result.toJson("deadbeef");
    assertEquals("success", rendered.getString("type"));
    assertEquals("A druggable surface antigen.", rendered.getJSONObject("ai_output").getString("headline"));
    assertEquals(row.getAiContent(), rendered.getJSONObject("ai_output").getString("content"));
  }

  @Test
  public void runSuccessRendersPdSectionIntoPersistedContent() throws Exception {
    JsonPromptClient client = (stage, repl) -> stage.equals("generatePDs")
        ? json("{\"PDs\": [{\"description\": \"Fructose 1,6-bisphosphatase FBP1\","
            + " \"evidence_code\": \"IDA\", \"code_reason\": \"Direct enzyme assay.\"}]}")
        : json("{\"only_in_passing\": false,"
            + "\"Headline\": \"A gluconeogenic enzyme.\","
            + "\"ShortSummary\": \"FBP1 matters.\","
            + "\"GeneSummary\": [{\"bullet_point\": \"FBP1 is a phosphatase.\","
            + "  \"evidence_location\": \"Fig 3\", \"supporting_quotes\": [\"assayed activity\"]}]}");
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "The gene PF3D7_1133400 (FBP1) is characterised here.", Arrays.asList("FBP1"),
        new PmcBiocFetcher(), client, store);

    pipeline.run();

    assertEquals(JobStatus.SUCCESS, pipeline.job().getStatus());
    String content = store.captured.getAiContent();
    // The PD section is rendered and sits between the Details and Evidence sections.
    int details = content.indexOf("Details:");
    int pds = content.indexOf("AI-suggested product description:");
    int evidence = content.indexOf("Evidence:");
    assertTrue("PD section present", pds >= 0);
    assertTrue("PD section after Details and before Evidence", details < pds && pds < evidence);
    assertTrue("PD line rendered",
        content.contains("- Fructose 1,6-bisphosphatase FBP1 [IDA]"));
  }

  @Test
  public void runPdFailureTerminatesInternalErrorAndPersistsNothing() throws Exception {
    JsonPromptClient client = (stage, repl) -> {
      if (stage.equals("generatePDs"))
        throw new WdkModelException("Max retries exceeded: could not parse valid JSON");
      return json("{\"only_in_passing\": false, \"Headline\": \"H\","
          + "\"GeneSummary\": [{\"bullet_point\": \"A finding.\"}]}");
    };
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "The gene PF3D7_1133400 is characterised here.", Collections.<String>emptyList(),
        new PmcBiocFetcher(), client, store);

    pipeline.run();

    assertEquals(JobStatus.INTERNAL_ERROR, pipeline.job().getStatus());
    assertEquals("a PD-less success must never be cached", 0, store.calls);
  }

  @Test
  public void runGeneNotMentionedPersistsRunRow() throws Exception {
    JsonPromptClient client = (stage, repl) -> { throw new AssertionError("LLM must not run"); };
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "This paper never names the gene.", Collections.<String>emptyList(),
        new PmcBiocFetcher(), client, store);

    pipeline.run();

    assertEquals(JobStatus.GENE_NOT_MENTIONED, pipeline.job().getStatus());
    assertEquals("gene-not-mentioned is persisted to the cache", 1, store.calls);
    CommentAiRun row = store.captured;
    assertEquals(JobStatus.GENE_NOT_MENTIONED, row.getTerminalStatus());
    assertNull(row.getAiHeadline());
    assertNull(row.getAiContent());
    assertFalse(row.isOnlyMentionedInPassing());
  }

  @Test
  public void runMentionedInPassingPersistsRunRow() throws Exception {
    JsonPromptClient client = (stage, repl) -> json("{\"only_in_passing\": true}");
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "PF3D7_1133400 appears once in passing.", Collections.<String>emptyList(),
        new PmcBiocFetcher(), client, store);

    pipeline.run();

    assertEquals(JobStatus.MENTIONED_IN_PASSING, pipeline.job().getStatus());
    assertEquals("mentioned-in-passing is persisted to the cache", 1, store.calls);
    CommentAiRun row = store.captured;
    assertEquals(JobStatus.MENTIONED_IN_PASSING, row.getTerminalStatus());
    assertTrue(row.isOnlyMentionedInPassing());
    assertNull(row.getAiHeadline());
  }

  @Test
  public void runTextUnavailableIsNeverPersisted() {
    CapturingStore store = new CapturingStore();
    AiGenePublicationPipeline pipeline = runnable("pubmed", "999", null,
        Collections.<String>emptyList(), unavailableFetcher("not open-access"),
        (stage, repl) -> null, store);

    pipeline.run();

    assertEquals(JobStatus.TEXT_UNAVAILABLE, pipeline.job().getStatus());
    assertEquals("non-cacheable terminals write nothing", 0, store.calls);
  }

  @Test
  public void runPersistFailureBecomesInternalError() throws Exception {
    JsonPromptClient client = (stage, repl) ->
        json("{\"only_in_passing\": false, \"ShortSummary\": \"x\"}");
    AiGenePublicationPipeline.AiRunStore boom = run -> { throw new WdkModelException("db down"); };
    AiGenePublicationPipeline pipeline = runnable("upload", null,
        "PF3D7_1133400 is studied.", Collections.<String>emptyList(),
        new PmcBiocFetcher(), client, boom);

    pipeline.run();

    assertEquals(JobStatus.INTERNAL_ERROR, pipeline.job().getStatus());
  }
}
