package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.BIGINT;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;

import java.sql.SQLException;
import java.sql.Timestamp;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.gusdb.fgputil.db.runner.ArgumentBatch;
import org.gusdb.fgputil.db.runner.ListArgumentBatch;

/**
 * Insert one row into {@code comment_ai_provenance} — the AI provenance row for
 * a published comment, keyed by {@code comment_id} with a FK to
 * {@code comment_ai_run.job_id}.
 *
 * <p>Run inside the same transaction as the {@code comments} insert (driven by
 * the publish endpoint on user approval) so each published AI-assisted comment
 * gains its provenance row atomically.
 */
public class InsertCommentAiProvenanceQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s." + Table.COMMENT_AI_PROVENANCE + " (" +
      "comment_id, run_job_id, is_edited, created_at" +
      ") VALUES (?, ?, ?, ?)";

  private static final Integer[] TYPES = { BIGINT, VARCHAR, BOOLEAN, TIMESTAMP };

  private final AiProvenance _provenance;

  public InsertCommentAiProvenanceQuery(String schema, AiProvenance provenance) {
    super(schema, Table.COMMENT_AI_PROVENANCE, null);
    _provenance = provenance;
  }

  @Override
  protected ArgumentBatch getArguments() throws SQLException {
    Timestamp createdAt = _provenance.getCreatedAt() == null ? null
        : new Timestamp(_provenance.getCreatedAt().getTime());

    ListArgumentBatch batch = new ListArgumentBatch();
    batch.add(new Object[] {
        _provenance.getCommentId(),
        _provenance.getRunJobId(),
        _provenance.isEdited(),
        createdAt
    });
    batch.setParameterTypes(TYPES);
    return batch;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
