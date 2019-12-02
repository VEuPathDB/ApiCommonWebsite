package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class ValueQuery<T> extends Query {

  private T _value;

  protected ValueQuery(String schema) {
    super(schema);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ValueQuery<T> run(Connection con) throws SQLException {
    return (ValueQuery<T>) super.run(con);
  }

  @Override
  protected void execute(SQLRunner runner) {
    runner.executeQuery(getParams(), getTypes(), this::handle);
  }

  public T value() {
    return _value;
  }

  private Void handle(ResultSet rs) throws SQLException {
    _value = parseResults(rs);
    return null;
  }

  protected abstract T parseResults(ResultSet rs) throws SQLException;

  protected abstract Object[] getParams();
  protected abstract Integer[] getTypes();
}
