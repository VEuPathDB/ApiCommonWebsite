package org.apidb.apicommon.model.comment.repo;

import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Types.BIGINT;

/**
 * Get whether or not a comment with the given id exists in
 * the comment table.
 */
public class GetCommentExistsQuery extends ValueQuery<Boolean> {
  private static final String SQL = "SELECT 1 FROM %s.COMMENTS WHERE COMMENT_ID = ?";

  private static final Integer[] TYPES = { BIGINT };

  private final long _commentId;

  public GetCommentExistsQuery(String schema, long commentId) {
    super(schema);
    _commentId = commentId;
  }

  @Override
  protected Boolean parseResults(ResultSet rs) throws SQLException {
    return rs.next();
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ _commentId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
