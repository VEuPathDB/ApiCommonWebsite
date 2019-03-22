package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.SQLException;
import java.util.Collection;

import static java.sql.Types.BIGINT;
import static java.sql.Types.INTEGER;

/**
 * Insert a link between a comment and selected categories.
 */
public class InsertCategoryQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s.COMMENTTARGETCATEGORY (" +
      "COMMENT_TARGET_CATEGORY_ID, COMMENT_ID, TARGET_CATEGORY_ID) " +
      "VALUES (?, ?, ?)";

  private static final Integer[] TYPES = { BIGINT, BIGINT, INTEGER };

  private final long _comId;

  private final Collection<Integer> _ids;

  public InsertCategoryQuery(String schema, long commentId,
      Collection<Integer> ids, IdSupplier idProvider) {
    super(schema, Table.COMMENT_TO_CATEGORY, idProvider);
    _comId = commentId;
    _ids = ids;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch batch = new BasicArgumentBatch();

    for (Integer id : _ids)
      batch.add(new Object[] { nextId(), _comId, id });

    batch.setParameterTypes(TYPES);

    return batch;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
