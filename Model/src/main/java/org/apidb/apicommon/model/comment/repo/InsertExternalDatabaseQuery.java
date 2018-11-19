package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.ExternalDatabase;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class InsertExternalDatabaseQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s.external_databases (\n" +
      "    external_database_id, external_database_name,\n" +
      "    external_database_version)\n" +
      "VALUES (?, ?, ?)";

  private static final Integer[] TYPE = { BIGINT, VARCHAR, VARCHAR };

  private final ExternalDatabase _extDb;
  private long _id;

  public InsertExternalDatabaseQuery(String schema, ExternalDatabase extDb,
      IdSupplier idProvider) {
    super(schema, Table.EXTERNAL_DB, idProvider);
    _extDb = extDb;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch out = new BasicArgumentBatch();
    out.add(new Object[]{ _id = nextId(), _extDb.getName(), _extDb.getVersion() });
    out.setParameterTypes(TYPE);
    return null;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  public InsertExternalDatabaseQuery run(Connection con) throws SQLException {
    return (InsertExternalDatabaseQuery) super.run(con);
  }

  public long value() {
    return _id;
  }
}
