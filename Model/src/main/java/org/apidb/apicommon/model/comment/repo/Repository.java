package org.apidb.apicommon.model.comment.repo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class Repository {
  protected final String _schema;
  protected final String _table;
  private final DataSource _ds;
  protected final InsertQuery.IdSupplier<String, Long> _seq;

  protected Repository(String schema, String table, DataSource ds,
      InsertQuery.IdSupplier<String, Long> seq) {
    _schema = schema;
    _table = table;
    _ds = ds;
    _seq = seq;
  }

  protected Connection connect() throws SQLException {
    return _ds.getConnection();
  }

  protected PreparedStatement prepare(Connection con, String sql) throws SQLException {
    return con.prepareStatement(format(sql));
  }

  protected long nextId() throws SQLException {
    return _seq.apply(_table);
  }

  protected String format(final String sql) {
    return String.format(sql, _schema, _table);
  }

}
