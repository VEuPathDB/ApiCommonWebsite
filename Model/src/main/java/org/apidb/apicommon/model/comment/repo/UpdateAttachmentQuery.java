package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;

public class UpdateAttachmentQuery extends Query {

  private static final String SQL = "UPDATE %s.COMMENTFILE SET COMMENT_ID = ?" +
      " WHERE COMMENT_ID = ?";

  private static final Integer[] TYPES = { BIGINT, BIGINT };

  private final long _oldId;
  private final long _newId;

  public UpdateAttachmentQuery(String schema, long oldId, long newId) {
    super(schema);
    _oldId = oldId;
    _newId = newId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected void execute(SQLRunner runner) {
    runner.executeStatement(new Object[]{ _newId, _oldId }, TYPES);
  }
}
