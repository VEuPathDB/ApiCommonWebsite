package org.apidb.apicommon.service.services.ai;

/**
 * The asynchronous pipeline (stages ①–⑥) that runs on the {@link JobRegistry}
 * bounded executor for a true cache miss. Each stage updates the shared
 * {@link JobState} progress via {@link JobState#updateStage} so polling GETs
 * observe advancement, and terminal outcomes are recorded via
 * {@link JobState#markTerminal}.
 *
 * <p>Stages: ① fetching-article, ② scanning-gene-mentions, ③ generating-summary,
 * ④ validating (iff {@code options.validate}), ⑤ flatten-to-comment,
 * ⑥ persisting (iff {@code options.create_user_comment}).
 */
public class AiGenePublicationPipeline implements Runnable {

  private final JobState _job;

  public AiGenePublicationPipeline(JobState job) {
    _job = job;
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
