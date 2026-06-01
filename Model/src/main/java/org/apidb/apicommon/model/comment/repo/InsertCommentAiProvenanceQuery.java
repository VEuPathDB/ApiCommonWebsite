package org.apidb.apicommon.model.comment.repo;

import java.sql.SQLException;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.gusdb.fgputil.db.runner.ArgumentBatch;

/**
 * Insert one row into {@code comment_ai_provenance} — the per-comment AI
 * provenance/review-state row, keyed by {@code comment_id} with a FK to
 * {@code comment_ai_run.job_id}.
 *
 * <p>Run inside the same transaction as the {@code comments} insert so each
 * AI-assisted comment gains its provenance row atomically (deliverable 6).
 *
 * <p>SCAFFOLDING: SQL shape is final; argument binding is wired in deliverable 6.
 */
public class InsertCommentAiProvenanceQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s." + Table.COMMENT_AI_PROVENANCE + " (" +
      "comment_id, run_job_id, review_level, reviewed_at" +
      ") VALUES (?, ?, ?, ?)";

  private final AiProvenance _provenance;

  public InsertCommentAiProvenanceQuery(String schema, AiProvenance provenance) {
    super(schema, Table.COMMENT_AI_PROVENANCE, null);
    _provenance = provenance;
  }

  @Override
  protected ArgumentBatch getArguments() throws SQLException {
    throw new UnsupportedOperationException(
        "InsertCommentAiProvenanceQuery argument binding — deliverable 6");
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
