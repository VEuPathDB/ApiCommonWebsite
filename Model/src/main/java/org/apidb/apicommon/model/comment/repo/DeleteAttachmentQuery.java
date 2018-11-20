package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;

/**
 * Delete link between comment and user file record by
 * comment and file id.
 */
public class DeleteAttachmentQuery extends Query {

  private static final String SQL = "DELETE FROM %s.COMMENTFILE\n" +
      "WHERE COMMENT_ID = ? AND FILE_ID = ?";

  private static final Integer[] TYPES = { BIGINT, BIGINT };

  private final long _commentId;
  private final long _fileId;

  public DeleteAttachmentQuery(String schema, long commentId, long fileId) {
    super(schema);
    _commentId = commentId;
    _fileId = fileId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected void execute(SQLRunner runner) {
    runner.executeStatement(new Object[]{ _commentId, _fileId }, TYPES);
  }
}
