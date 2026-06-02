package org.apidb.apicommon.model.comment.repo;

import java.sql.SQLException;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.gusdb.fgputil.db.runner.ArgumentBatch;

/**
 * Insert one row into {@code comment_ai_provenance} — the AI provenance row for
 * a published comment, keyed by {@code comment_id} with a FK to
 * {@code comment_ai_run.job_id}.
 *
 * <p>Run inside the same transaction as the {@code comments} insert (driven by
 * the publish endpoint on user approval) so each published AI-assisted comment
 * gains its provenance row atomically.
 *
 * <p>SCAFFOLDING: SQL shape is final; argument binding is wired with the publish
 * endpoint deliverable.
 */
public class InsertCommentAiProvenanceQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s." + Table.COMMENT_AI_PROVENANCE + " (" +
      "comment_id, run_job_id, is_edited, created_at" +
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
