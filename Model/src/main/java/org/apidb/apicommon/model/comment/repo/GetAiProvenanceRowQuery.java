package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.BIGINT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.apidb.apicommon.model.comment.pojo.AiProvenance;

/**
 * Look up the raw {@code comment_ai_provenance} row for a single comment id,
 * returning the {@code run_job_id} FK that the client-facing
 * {@link GetCommentAiProvenanceQuery} (which yields an {@code AiProvenanceView})
 * deliberately omits.
 *
 * <p>Used by the edit-comment flow to carry an AI comment's provenance forward
 * onto the replacement comment: the FK lets the new comment re-link to the same
 * shared {@code comment_ai_run} row.
 *
 * <p>Follows the {@link ValueQuery} read pattern ({@code parseResults} /
 * {@code getParams} / {@code getTypes}).
 */
public class GetAiProvenanceRowQuery extends ValueQuery<Optional<AiProvenance>> {

  private static final String SQL = "SELECT comment_id, run_job_id, is_edited, created_at" +
      " FROM %s." + Table.COMMENT_AI_PROVENANCE +
      " WHERE comment_id = ?";

  private static final Integer[] TYPES = { BIGINT };

  private final long _commentId;

  public GetAiProvenanceRowQuery(String schema, long commentId) {
    super(schema);
    _commentId = commentId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected Optional<AiProvenance> parseResults(ResultSet rs) throws SQLException {
    if (!rs.next())
      return Optional.empty();

    return Optional.of(new AiProvenance()
        .setCommentId(rs.getLong("comment_id"))
        .setRunJobId(rs.getString("run_job_id"))
        .setEdited(rs.getBoolean("is_edited"))
        .setCreatedAt(rs.getTimestamp("created_at")));
  }

  @Override
  protected Object[] getParams() {
    return new Object[] { _commentId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }
}
