package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * ④ validating (iff {@code options.validate}), ⑤ flatten-to-comment,
 * ⑥ persisting (writes the {@code comment_ai_run} cache row only — no comment is
 * created here; that happens later on user approval via the publish endpoint).
 */
public class AiGenePublicationPipeline implements Runnable {

  /** Verbatim quotes per bullet point requested in the prompt (Python {@code N_QUOTES}). */
  private static final String N_QUOTES = "2";

  private final JobState _job;
  private final WdkModel _wdkModel;
  private final PmcBiocFetcher _fetcher;
  private final GeneMentionScanner _scanner;
  private JsonPromptClient _summaryClient;     // built lazily from WdkModel, or injected for tests

  // --- transient per-stage outputs, threaded ① → ⑥ -------------------------
  private String _articleText;                 // ① resolved text (fetched for pubmed; uploaded text for upload)
  private List<String> _namesMentioned;        // ② gene + top-3 aliases actually mentioned (prompt input)
  private JsonNode _summaryJson;               // ③ getGeneSummary response
  private JsonNode _validatedJson;             // ④ verifyGeneSummary response (iff validate)
  private String _aiHeadline;                  // ⑤ flattened headline
  private String _aiContent;                   // ⑤ flattened content

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
    _job = job;
    _wdkModel = wdkModel;
    _fetcher = fetcher;
    _scanner = scanner;
    _summaryClient = summaryClient;
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

  @Override
  public void run() {
    try {
      fetchArticle();
      if (_job.getStatus().isTerminal()) return;

      scanGeneMentions();
      if (_job.getStatus().isTerminal()) return;

      generateSummary();
      if (_job.getStatus().isTerminal()) return;

      if (_job.getSubmission().getOptions().validate) {
        validateSummary();
        if (_job.getStatus().isTerminal()) return;
      }

      flattenToComment();

      // Always write the comment_ai_run cache row for a cacheable success; the
      // user-comment row is created later by the publish endpoint, not here.
      persist();
    }
    catch (Throwable t) {
      // Any unhandled stage failure terminates the job as internal-error rather
      // than leaving it stuck in `running`. (During deliverables 3-6 the not-yet-
      // implemented stages throw UnsupportedOperationException and land here.)
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
  void validateSummary() {
    throw new UnsupportedOperationException("validateSummary — deliverable 5");
  }

  // ⑤ -------------------------------------------------------------------------
  void flattenToComment() {
    throw new UnsupportedOperationException("flattenToComment — deliverable 5");
  }

  // ⑥ -------------------------------------------------------------------------
  void persist() {
    throw new UnsupportedOperationException("persist — deliverable 6");
  }
}
