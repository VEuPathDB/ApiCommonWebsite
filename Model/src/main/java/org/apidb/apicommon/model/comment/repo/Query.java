package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.Connection;
import java.sql.SQLException;

abstract class Query {
  private final String _schema;

  protected Query(String schema) {
    _schema = schema;
  }

  protected abstract String getQuery();

  protected abstract void execute(SQLRunner runner) throws SQLException;

  public Query run(Connection con) throws SQLException {
    execute(new SQLRunner(con, format(getQuery())));
    return this;
  }

  protected String format(String sql) {
    return String.format(sql, _schema);
  }
}
