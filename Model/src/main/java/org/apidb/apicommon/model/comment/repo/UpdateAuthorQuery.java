package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Author;
import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class UpdateAuthorQuery extends Query {

  private static final String QUERY = "UPDATE %s.COMMENT_USERS\n" +
      "SET FIRST_NAME = ?, LAST_NAME = ?, ORGANIZATION = ?\n" +
      "WHERE USER_ID = ?";

  private static final Integer[] TYPES = { VARCHAR, VARCHAR, VARCHAR, BIGINT };

  private final Author _author;

  public UpdateAuthorQuery(String schema, Author author) {
    super(schema);
    _author = author;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }

  @Override
  protected void execute(SQLRunner runner) {
    runner.executeStatement(
        new Object[] { _author.getFirstName(), _author.getLastName(),
            _author.getOrganization(), _author.getUserId() }, TYPES);
  }
}
