package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.SQLException;
import java.util.Collection;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class InsertStableIdQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s.COMMENTSTABLEID (\n" +
      "    COMMENT_STABLE_ID, STABLE_ID, COMMENT_ID)\n" +
      "VALUES (?, ?, ?)";

  private static final Integer[] TYPES = { BIGINT, VARCHAR, VARCHAR };
  private final long _commentId;
  private final Collection<String> _stableId;

  public InsertStableIdQuery(String schema, long commentId,
      Collection<String> stableId,
      IdSupplier idProvider) {
    super(schema, Table.COMMENT_TO_STABLE_ID, idProvider);
    _commentId = commentId;
    _stableId = stableId;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch out = new BasicArgumentBatch();
    for (String id : _stableId)
      out.add(new Object[] { nextId(), id, _commentId });
    out.setParameterTypes(TYPES);
    return out;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
