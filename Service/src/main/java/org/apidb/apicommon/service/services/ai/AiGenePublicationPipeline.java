package org.apidb.apicommon.service.services.ai;

import java.util.Map;

import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher;
import org.apidb.apicommon.service.services.ai.article.PmcBiocFetcher.TextUnavailableException;
import org.gusdb.wdk.model.WdkModel;

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
 * ⑥ persisting (iff {@code options.create_user_comment}).
 */
public class AiGenePublicationPipeline implements Runnable {

  private final JobState _job;
  private final WdkModel _wdkModel;
  private final PmcBiocFetcher _fetcher;

  // --- transient per-stage outputs, threaded ① → ⑥ -------------------------
  private String _articleText;                 // ① resolved text (fetched for pubmed; uploaded text for upload)
  private Map<String, Integer> _mentionCounts; // ② per-synonym hit counts
  private JsonNode _summaryJson;               // ③ getGeneSummary response
  private JsonNode _validatedJson;             // ④ verifyGeneSummary response (iff validate)
  private String _aiHeadline;                  // ⑤ flattened headline
  private String _aiContent;                   // ⑤ flattened content

  public AiGenePublicationPipeline(JobState job, WdkModel wdkModel) {
    this(job, wdkModel, new PmcBiocFetcher());
  }

  /** Package-private seam: inject a {@link PmcBiocFetcher} to avoid network in tests. */
  AiGenePublicationPipeline(JobState job, WdkModel wdkModel, PmcBiocFetcher fetcher) {
    _job = job;
    _wdkModel = wdkModel;
    _fetcher = fetcher;
  }

  /** Test accessor for the stage-① resolved article text. */
  String articleText() {
    return _articleText;
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

      if (_job.getSubmission().getOptions().createUserComment) {
        persist();
      }
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
    throw new UnsupportedOperationException("scanGeneMentions — deliverable 3");
  }

  // ③ -------------------------------------------------------------------------
  void generateSummary() {
    throw new UnsupportedOperationException("generateSummary — deliverable 4");
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
