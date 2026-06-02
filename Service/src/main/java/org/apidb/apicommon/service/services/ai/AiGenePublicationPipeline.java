package org.apidb.apicommon.service.services.ai;

import java.util.Map;

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

  // --- transient per-stage outputs, threaded ① → ⑥ -------------------------
  private String _articleText;                 // ① resolved text (fetched for pubmed; uploaded text for upload)
  private Map<String, Integer> _mentionCounts; // ② per-synonym hit counts
  private JsonNode _summaryJson;               // ③ getGeneSummary response
  private JsonNode _validatedJson;             // ④ verifyGeneSummary response (iff validate)
  private String _aiHeadline;                  // ⑤ flattened headline
  private String _aiContent;                   // ⑤ flattened content

  public AiGenePublicationPipeline(JobState job, WdkModel wdkModel) {
    _job = job;
    _wdkModel = wdkModel;
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException(
        "AiGenePublicationPipeline.run — wired across deliverables 2-6");
  }

  // ① -------------------------------------------------------------------------
  void fetchArticle() {
    throw new UnsupportedOperationException("fetchArticle — deliverable 2");
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
