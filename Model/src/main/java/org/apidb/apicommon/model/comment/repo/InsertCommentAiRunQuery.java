package org.apidb.apicommon.model.comment.repo;

import java.sql.SQLException;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.gusdb.fgputil.db.runner.ArgumentBatch;

/**
 * Insert one row into {@code comment_ai_run} — the shared LLM-output cache,
 * keyed by the content-digest {@code job_id}. Idempotent at the application
 * level: the digest dedupes resubmissions, so a row is written at most once per
 * distinct job.
 *
 * <p>Follows the {@link InsertCategoryQuery} / {@link InsertAttachmentQuery}
 * pattern. {@code job_id} is the supplied primary key (no sequence), so the
 * {@link IdSupplier} is {@code null}.
 *
 * <p>SCAFFOLDING: SQL shape is final; argument binding (notably the
 * {@code synonyms_used TEXT[]} PostgreSQL array) is wired in deliverable 6.
 */
public class InsertCommentAiRunQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s." + Table.COMMENT_AI_RUN + " (" +
      "job_id, model_name, prompt_version, source_kind, pubmed_id, " +
      "external_url, external_title, pdf_content_sha256, gene_id, synonyms_used, " +
      "options_json, terminal_status, is_only_mentioned_in_passing, " +
      "ai_headline, ai_content, completed_at" +
      ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final CommentAiRun _run;

  public InsertCommentAiRunQuery(String schema, CommentAiRun run) {
    super(schema, Table.COMMENT_AI_RUN, null);
    _run = run;
  }

  @Override
  protected ArgumentBatch getArguments() throws SQLException {
    throw new UnsupportedOperationException(
        "InsertCommentAiRunQuery argument binding — deliverable 6");
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
