package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Author;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.BIGINT;

public class GetAuthorQuery extends ValueQuery<Optional<Author>> {

  private static final String QUERY = "SELECT *\n" +
      "FROM %s.COMMENT_USERS\n" + "WHERE USER_ID = ?";

  private static final Integer[] TYPES = { BIGINT };

  private long userId;

  public GetAuthorQuery(String schema, long id) {
    super(schema);
    userId = id;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }

  @Override
  protected Optional<Author> parseResults(ResultSet rs) throws SQLException {
    if (!rs.next())
      return Optional.empty();

    return Optional.of(rs2Author(rs));
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ userId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }

  /**
   * ResultSet to Comment Author
   *
   * Parses columns out of a result set row to create an instance of Author.
   *
   * @param rs ResultSet handle on a DB row.
   *
   * @return Newly created author from the given row.
   */
  static Author rs2Author(final ResultSet rs) throws SQLException {
    return new Author()
        .setUserId(rs.getLong(Column.CommentUser.USER_ID))
        .setFirstName(rs.getString(Column.CommentUser.FIRST_NAME))
        .setLastName(rs.getString(Column.CommentUser.LAST_NAME))
        .setOrganization(rs.getString(Column.CommentUser.ORGANIZATION));
  }
}
