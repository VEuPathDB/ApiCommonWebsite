package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Attachment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

import static java.sql.Types.BIGINT;

/**
 * Get the link between a comment and a file if such a link
 * exists.
 */
public class GetAllAttachmentsQuery extends ValueQuery<Collection<Attachment>> {

  private static final String SQL = "SELECT *\n" +
      "FROM %s.COMMENTFILE\n" + "WHERE COMMENT_ID = ?";

  private static final Integer[] TYPES = { BIGINT };

  private long _commentId;

  public GetAllAttachmentsQuery(String schema, long commentId) {
    super(schema);
    _commentId = commentId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected Collection<Attachment> parseResults(ResultSet rs) throws SQLException {
    final Collection<Attachment> out = new HashSet<>();

    while(rs.next())
      out.add(rs2Attachment(rs));

    return out;
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ _commentId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }

  static Attachment rs2Attachment(final ResultSet rs) throws SQLException {
    return new Attachment()
        .setId(rs.getLong(Column.CommentFile.ID))
        .setName(rs.getString(Column.CommentFile.NAME))
        .setDescription(rs.getString(Column.CommentFile.NOTES));
  }
}
