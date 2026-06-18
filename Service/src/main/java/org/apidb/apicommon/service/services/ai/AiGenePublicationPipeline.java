package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apidb.apicommon.controller.CommentFactoryManager;
import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.apidb.apicommon.service.services.ai.gene.GeneMentionScanner;
import org.apidb.apicommon.service.services.ai.llm.AnthropicJsonClient;
import org.apidb.apicommon.service.services.ai.llm.JsonPromptClient;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The asynchronous pipeline (stages ①–⑥) that runs on the {@link JobRegistry}
 * bounded executor for a true cache miss. Owns exactly one {@link JobState} and
 * runs on a single thread, so per-stage intermediate outputs are held as
 * instance fields and threaded from stage to stage; only progress and the
 * terminal result are published back to the shared {@link JobState} (which
 * polling GETs read) via {@link JobState#updateStage} / {@link JobState#markTerminal}.
 *
 * <p>Shared, immutable inputs (gene, synonyms, source, model, options) are read
 * from {@code _job.getSubmission()}. Collaborators (article fetcher, mention
 * scanner, LLM client, comment factory) are constructed from {@link WdkModel}
 * as the relevant deliverables land.
 *
 * <p>Stages: ① fetching-article, ② scanning-gene-mentions, ③ generating-summary,
 * ④ flatten-to-comment, ⑤ persisting (writes the {@code comment_ai_run} cache
 * row only — no comment is created here; that happens later on user approval via
 * the publish endpoint). (A verifyGeneSummary validation stage was dropped on
 * 2026-06-05 — the Python authors found it didn't improve results enough to
 * justify the tokens.)
 */
public class AiGenePublicationPipeline implements Runnable {

  /** Verbatim quotes per bullet point requested in the prompt (Python {@code N_QUOTES}). */
  private static final String N_QUOTES = "2";

  /**
   * Narrow seam over {@link org.apidb.apicommon.model.comment.CommentFactory#persistAiRun}
   * so tests can capture the persisted row without a live database.
   */
  @FunctionalInterface
  public interface AiRunStore {
    void persist(CommentAiRun run) throws WdkModelException;
  }

  private final JobState _job;
  private final WdkModel _wdkModel;
  private final PmcBiocFetcher _fetcher;
  private final GeneMentionScanner _scanner;
  private JsonPromptClient _summaryClient;     // built lazily from WdkModel, or injected for tests
  private AiRunStore _aiRunStore;              // built lazily from WdkModel, or injected for tests

  // --- transient per-stage outputs, threaded ① → ⑥ -------------------------
  private String _articleText;                 // ① resolved text (fetched for pubmed; uploaded text for upload)
  private List<String> _namesMentioned;        // ② gene + top-3 aliases actually mentioned (prompt input)
  private JsonNode _summaryJson;               // ③ getGeneSummary response
  private String _aiHeadline;                  // ④ flattened headline
  private String _aiContent;                   // ④ flattened content

  public AiGenePublicationPipeline(JobState job, WdkModel wdkModel) {
    this(job, wdkModel, new PmcBiocFetcher(), new GeneMentionScanner());
  }

  /** Package-private seam: inject collaborators to avoid network in tests. */
  AiGenePublicationPipeline(JobState job, WdkModel wdkModel, PmcBiocFetcher fetcher) {
    this(job, wdkModel, fetcher, new GeneMentionScanner());
  }

  AiGenePublicationPipeline(JobState job, WdkModel wdkModel, PmcBiocFetcher fetcher,
      GeneMentionScanner scanner) {
    this(job, wdkModel, fetcher, scanner, null);
  }

  AiGenePublicationPipeline(JobState job, WdkModel wdkModel, PmcBiocFetcher fetcher,
      GeneMentionScanner scanner, JsonPromptClient summaryClient) {
    this(job, wdkModel, fetcher, scanner, summaryClient, null);
  }

  AiGenePublicationPipeline(JobState job, WdkModel wdkModel, PmcBiocFetcher fetcher,
      GeneMentionScanner scanner, JsonPromptClient summaryClient, AiRunStore aiRunStore) {
    _job = job;
    _wdkModel = wdkModel;
    _fetcher = fetcher;
    _scanner = scanner;
    _summaryClient = summaryClient;
    _aiRunStore = aiRunStore;
  }

  /** Test accessor for the owning job state. */
  JobState job() {
    return _job;
  }

  /** Test accessor for the stage-① resolved article text. */
  String articleText() {
    return _articleText;
  }

  /** Test accessor for the stage-② resolved names mentioned (prompt input). */
  List<String> namesMentioned() {
    return _namesMentioned;
  }

  /** Test accessor for the stage-③ parsed getGeneSummary response. */
  JsonNode summaryJson() {
    return _summaryJson;
  }

  /** Test accessors for the stage-④ flattened comment fields. */
  String aiHeadline() {
    return _aiHeadline;
  }

  String aiContent() {
    return _aiContent;
  }

  @Override
  public void run() {
    try {
      fetchArticle();
      if (!_job.getStatus().isTerminal()) scanGeneMentions();
      if (!_job.getStatus().isTerminal()) generateSummary();
      if (!_job.getStatus().isTerminal()) flattenToComment();

      // Persist runs for every cacheable terminal — the success path plus the
      // deterministic gene-not-mentioned / mentioned-in-passing short-circuits —
      // and is a no-op for the non-cacheable ones. It is the pipeline's only
      // write; the user-comment row is created later by the publish endpoint.
      persist();
    }
    catch (Throwable t) {
      // Any unhandled stage (or persist) failure terminates the job as
      // internal-error rather than leaving it stuck in `running`.
      _job.markTerminal(JobStatus.INTERNAL_ERROR,
          TerminalResult.internalError(t.getMessage()));
    }
  }

  // ① -------------------------------------------------------------------------
  void fetchArticle() {
    _job.updateStage(JobState.Stage.FETCHING_ARTICLE, "Resolving article text");
    JobSubmission submission = _job.getSubmission();

    // Upload path: the front-end already extracted the text (MuPDF.js); nothing
    // to fetch. The stage is still emitted for symmetry but completes at once.
    if ("upload".equals(submission.getSourceKind())) {
      _articleText = submission.getUploadedPaperText();
      return;
    }

    // PubMed path: fetch and parse the PMC BioC document.
    try {
      _articleText = _fetcher.fetch(submission.getPubmedId());
    }
    catch (TextUnavailableException e) {
      _job.markTerminal(JobStatus.TEXT_UNAVAILABLE,
          TerminalResult.textUnavailable(e.getMessage()));
    }
  }

  // ② -------------------------------------------------------------------------
  void scanGeneMentions() {
    _job.updateStage(JobState.Stage.SCANNING_GENE_MENTIONS, "Scanning for gene mentions");
    JobSubmission submission = _job.getSubmission();

    _namesMentioned = _scanner.namesMentioned(
        _articleText, submission.getGeneId(), submission.getSynonyms());

    // Neither the gene id nor any alias appears → deterministic terminal. The
    // synonyms_checked list records everything we searched for (gene id first).
    if (_namesMentioned.isEmpty()) {
      List<String> synonymsChecked = new ArrayList<>();
      synonymsChecked.add(submission.getGeneId());
      synonymsChecked.addAll(submission.getSynonyms());
      _job.markTerminal(JobStatus.GENE_NOT_MENTIONED,
          TerminalResult.geneNotMentioned(synonymsChecked));
    }
  }

  // ③ -------------------------------------------------------------------------
  void generateSummary() throws WdkModelException {
    _job.updateStage(JobState.Stage.GENERATING_SUMMARY, "Generating gene summary");
    JobSubmission submission = _job.getSubmission();

    Map<String, String> replacements = new HashMap<>();
    replacements.put("N_QUOTES", N_QUOTES);
    replacements.put("GENE", geneForPrompt(submission.getGeneId(), _namesMentioned));
    replacements.put("PAPER_TEXT", _articleText);

    _summaryJson = summaryClient().complete("getGeneSummary", replacements);

    // only_in_passing=true → deterministic short-circuit to mentioned-in-passing
    // (persisted to comment_ai_run, like gene-not-mentioned).
    JsonNode flag = _summaryJson.get("only_in_passing");
    if (flag != null && flag.asBoolean()) {
      List<String> synonymsChecked = new ArrayList<>();
      synonymsChecked.add(submission.getGeneId());
      synonymsChecked.addAll(submission.getSynonyms());
      _job.markTerminal(JobStatus.MENTIONED_IN_PASSING,
          TerminalResult.mentionedInPassing(synonymsChecked));
    }
  }

  private JsonPromptClient summaryClient() throws WdkModelException {
    if (_summaryClient == null)
      _summaryClient = new AnthropicJsonClient(_wdkModel);
    return _summaryClient;
  }

  /**
   * Build the gene name fed into the prompt (port of {@code gene_for_prompt}):
   * the sole name when there is only one; otherwise {@code "<geneId> (also known
   * as X or Y)"} with the gene id first when it is among the mentioned names, or
   * the first mention followed by the rest when it is not.
   */
  static String geneForPrompt(String geneId, List<String> names) {
    if (names == null || names.isEmpty()) return geneId;
    if (names.size() == 1) return names.get(0);

    String gidLower = geneId.toLowerCase();
    boolean geneAmongNames = names.stream().anyMatch(n -> n.toLowerCase().equals(gidLower));

    String head;
    List<String> rest = new ArrayList<>();
    if (geneAmongNames) {
      head = geneId;
      for (String n : names)
        if (!n.toLowerCase().equals(gidLower)) rest.add(n);
    }
    else {
      head = names.get(0);
      rest.addAll(names.subList(1, names.size()));
    }
    return head + " (also known as " + String.join(" or ", rest) + ")";
  }

  // ④ -------------------------------------------------------------------------
  void flattenToComment() {
    _aiHeadline = flattenHeadline(_summaryJson);
    _aiContent = flattenContent(_summaryJson);
  }

  /** Headline = the plain-text {@code Headline} (empty if absent). */
  static String flattenHeadline(JsonNode summary) {
    return text(summary, "Headline");
  }

  /**
   * Body = an "Executive summary:" section ({@code ShortSummary}), a "Details:"
   * section of {@code - } bullets each tagged with a {@code [n]} reference, an
   * "Evidence:" section pairing each {@code [n]} with its location and {@code - }
   * quote lines, then the existing "Additional inferences" and "Aliases mentioned
   * in paper" sections appended unchanged — sections separated by a blank line.
   * Plain-text markdown only (no HTML); client renders newlines as {@code <br />}.
   */
  static String flattenContent(JsonNode summary) {
    List<String> sections = new ArrayList<>();

    // 1. executive summary (the one-sentence ShortSummary)
    String shortSummary = text(summary, "ShortSummary");
    if (!shortSummary.isEmpty()) {
      sections.add("Executive summary:\n\n" + shortSummary);
    }

    // 2. gene-summary bullets ("Details:") cross-referenced to their evidence
    //    ("Evidence:") by a shared [n] tag. Every bullet is numbered; an evidence
    //    stanza is emitted only when the bullet carries a location and/or quotes.
    StringBuilder details = new StringBuilder();
    StringBuilder evidence = new StringBuilder();
    int n = 0;
    for (JsonNode row : array(summary, "GeneSummary")) {
      n++;
      if (details.length() > 0) details.append("\n");
      details.append("- ").append(text(row, "bullet_point")).append(" [").append(n).append("]");

      String loc = text(row, "evidence_location");
      List<JsonNode> quotes = new ArrayList<>();
      for (JsonNode quote : array(row, "supporting_quotes")) quotes.add(quote);
      if (loc.isEmpty() && quotes.isEmpty()) continue;

      if (evidence.length() > 0) evidence.append("\n\n");
      evidence.append("[").append(n).append("] ").append(loc);
      for (JsonNode quote : quotes) {
        evidence.append("\n- ").append(quote.asText());
      }
    }
    if (details.length() > 0) sections.add("Details:\n\n" + details);
    if (evidence.length() > 0) sections.add("Evidence:\n\n" + evidence);

    // 3. additional inferences
    StringBuilder inferences = new StringBuilder();
    for (JsonNode inf : array(summary, "AdditionalInferences")) {
      inferences.append(inferences.length() == 0 ? "Additional inferences:\n\n" : "\n");
      inferences.append("- ").append(inf.asText());
    }
    if (inferences.length() > 0) sections.add(inferences.toString());

    // 4. aliases mentioned in the paper
    List<String> aliases = new ArrayList<>();
    for (JsonNode a : array(summary, "Aliases_in_paper")) {
      aliases.add(a.asText());
    }
    if (!aliases.isEmpty()) {
      sections.add("Aliases mentioned in paper: " + String.join(", ", aliases));
    }

    return String.join("\n\n", sections);
  }

  /** A text field on a node, or {@code ""} when absent/null. */
  private static String text(JsonNode node, String field) {
    JsonNode v = node == null ? null : node.get(field);
    return v == null || v.isNull() ? "" : v.asText();
  }

  /** An array field on a node, or an empty iterable when absent/not an array. */
  private static Iterable<JsonNode> array(JsonNode node, String field) {
    JsonNode v = node == null ? null : node.get(field);
    return v != null && v.isArray() ? v : java.util.Collections.<JsonNode>emptyList();
  }

  // ⑤ -------------------------------------------------------------------------
  /**
   * Write the shared {@code comment_ai_run} cache row for a cacheable terminal
   * (success / gene-not-mentioned / mentioned-in-passing) and, on the success
   * path, publish the terminal {@code ai_output}. Non-cacheable terminals
   * (text-unavailable / internal-error / cancelled) write nothing — retries are
   * free, so they are never cached. This is the pipeline's only write; the
   * user-comment row is created later by the publish endpoint on user approval.
   */
  void persist() throws WdkModelException {
    // A still-RUNNING job here means the staged section completed without any
    // early terminal — i.e. the success path. Otherwise a stage already marked
    // the deterministic terminal outcome we are about to cache.
    boolean successPath = !_job.getStatus().isTerminal();
    JobStatus terminal = successPath ? JobStatus.SUCCESS : _job.getStatus();

    if (terminal != JobStatus.SUCCESS
        && terminal != JobStatus.GENE_NOT_MENTIONED
        && terminal != JobStatus.MENTIONED_IN_PASSING)
      return;  // text-unavailable / internal-error / cancelled: never cached

    if (successPath)
      _job.updateStage(JobState.Stage.PERSISTING, "Saving result");

    aiRunStore().persist(buildRun(terminal));

    if (successPath)
      _job.markTerminal(JobStatus.SUCCESS, TerminalResult.success(_aiHeadline, _aiContent));
  }

  /** Map the immutable submission plus the flattened outputs onto a cache row. */
  private CommentAiRun buildRun(JobStatus terminal) {
    JobSubmission s = _job.getSubmission();
    boolean success = terminal == JobStatus.SUCCESS;
    return new CommentAiRun()
        .setJobId(s.getJobId())
        .setModelName(s.getModelName())
        .setPromptVersion(s.getPromptVersion())
        .setSourceKind(s.getSourceKind())
        .setPubmedId(s.getPubmedId())
        .setExternalUrl(s.getExternalUrl())
        .setExternalTitle(s.getExternalTitle())
        .setPdfContentSha256(s.getPdfContentSha256())
        .setGeneId(s.getGeneId())
        .setSynonymsUsed(s.getSynonyms())
        .setOptionsJson(s.getOptionsJson())
        .setTerminalStatus(terminal.getWireValue())
        .setOnlyMentionedInPassing(terminal == JobStatus.MENTIONED_IN_PASSING)
        .setAiHeadline(success ? _aiHeadline : null)
        .setAiContent(success ? _aiContent : null)
        .setCompletedAt(new Date());
  }

  private AiRunStore aiRunStore() {
    if (_aiRunStore == null)
      _aiRunStore = CommentFactoryManager
          .getCommentFactory(_wdkModel.getProjectId())::persistAiRun;
    return _aiRunStore;
  }
}
