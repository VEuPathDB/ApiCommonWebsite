package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Author;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

/**
 * Insert comment author details
 */
public class InsertAuthorQuery extends InsertQuery {
  private static final String QUERY = "INSERT INTO %s.COMMENT_USERS (\n" +
      "USER_ID, FIRST_NAME, LAST_NAME, ORGANIZATION)\n" +
      "VALUES (?, ?, ?, ?)";

  private static final Integer[] TYPES = {BIGINT, VARCHAR, VARCHAR, VARCHAR};
  private final Author _author;

  public InsertAuthorQuery(String schema, Author author) {
    super(schema, Table.COMMENT_USERS, null);
    _author = author;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() {
    final BasicArgumentBatch batch = new BasicArgumentBatch();
    batch.add(new Object[]{ _author.getUserId(), _author.getFirstName(),
        _author.getLastName(), _author.getOrganization() });
    batch.setParameterTypes(TYPES);
    return batch;
  }
}
