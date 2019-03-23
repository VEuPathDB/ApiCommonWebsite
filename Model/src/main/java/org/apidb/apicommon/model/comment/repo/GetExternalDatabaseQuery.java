package org.apidb.apicommon.model.comment.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.VARCHAR;

/**
 * Look up external database id by name and version.
 */
public class GetExternalDatabaseQuery extends ValueQuery<Optional<Long>> {

  private static final String SQL = "SELECT EXTERNAL_DATABASE_ID\n" +
      "FROM %s.EXTERNAL_DATABASES\n" +
      "WHERE EXTERNAL_DATABASE_NAME = ?\n" +
      "AND EXTERNAL_DATABASE_VERSION = ?";

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
