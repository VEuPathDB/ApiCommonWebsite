package org.apidb.apicommon.model.comment.repo;

import io.vulpine.lib.jcfie.ExtensibleCheckedFunction;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.SQLException;

abstract class InsertQuery extends Query {
  private final String _table;
  private final IdSupplier _idProvider;

  protected InsertQuery(String schema, String table,
      IdSupplier idProvider) {
    super(schema);
    _table = table;
    _idProvider = idProvider;
  }

  protected abstract SQLRunner.ArgumentBatch getArguments() throws SQLException;

  @Override
  protected void execute(SQLRunner runner) throws SQLException {
    runner.executeStatementBatch(getArguments());
  }

  protected long nextId() throws SQLException {
    return _idProvider.apply(_table);
  }

  public interface IdSupplier extends
      ExtensibleCheckedFunction<String, Long, SQLException> {}
}
