package org.apidb.apicommon.model.comment.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.VARCHAR;

public class GetExternalDatabaseQuery extends ValueQuery<Optional<Long>> {

  private static final String SQL = "SELECT external_database_id\n" +
      "FROM %s.external_databases\n" +
      "WHERE external_database_name = ?\n" +
      "AND external_database_version = ?";

  private static final Integer[] TYPES = { VARCHAR, VARCHAR };

  private final String _name;
  private final String _version;

  public GetExternalDatabaseQuery(String schema, String name, String version) {
    super(schema);
    _name = name;
    _version = version;
  }

  @Override
  protected Optional<Long> parseResults(ResultSet rs)
      throws SQLException {
    if(!rs.next())
      return Optional.empty();

    return Optional.of(rs.getLong(Column.ExternalDatabase.ID));
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ _name, _version };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
