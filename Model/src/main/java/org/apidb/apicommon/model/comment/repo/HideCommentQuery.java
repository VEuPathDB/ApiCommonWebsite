package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;

public class HideCommentQuery extends Query {

  private static final String SQL = "UPDATE %s.COMMENTS\n" +
      "SET IS_VISIBLE = 0\n" +
      "WHERE comment_id = ?";

  private static final Integer[] TYPES = { BIGINT };

  private final long _commentId;

  public HideCommentQuery(String schema, long commentId) {
    super(schema);
    _commentId = commentId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected void execute(SQLRunner runner) {
    runner.executeStatement(new Object[]{ _commentId }, TYPES);
  }
}
