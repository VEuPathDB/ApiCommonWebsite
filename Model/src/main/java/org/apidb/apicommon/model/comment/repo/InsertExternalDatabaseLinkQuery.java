package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;

/**
 * Insert a link between a comment and an external database.
 */
public class InsertExternalDatabaseLinkQuery extends InsertQuery {
  private static final String SQL = "INSERT INTO %s.COMMENT_EXTERNAL_DATABASE (\n" +
      "    EXTERNAL_DATABASE_ID, COMMENT_ID)\n" +
      "VALUES (?, ?)";

  private static final Integer[] TYPES = { BIGINT, BIGINT };
  private final long _commentId;
  private final long _extDbId;

  public InsertExternalDatabaseLinkQuery(String schema, long commentId,
      long extDbId) {
    super(schema, Table.COMMENT_TO_EXT_DB, null);
    _commentId = commentId;
    _extDbId = extDbId;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() {
    final BasicArgumentBatch out = new BasicArgumentBatch();
    out.add(new Object[]{ _extDbId, _commentId });
    out.setParameterTypes(TYPES);
    return out;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
