package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Attachment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.BIGINT;

public class GetAttachmentQuery extends ValueQuery<Optional<Attachment>> {

  private static final String SQL = "SELECT *\n" +
      "FROM %s.COMMENTFILE\n" + "WHERE FILE_ID = ? AND COMMENT_ID = ?";

  private static final Integer[] TYPES = { BIGINT, BIGINT };

  private long _fileId;
  private long _commentId;

  public GetAttachmentQuery(String schema, long commentId, long fileId) {
    super(schema);
    _commentId = commentId;
    _fileId = fileId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected Optional<Attachment> parseResults(ResultSet rs) throws SQLException {
    if (!rs.next())
      return Optional.empty();

    return Optional.of(rs2Attachment(rs));
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ _fileId, _commentId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }

  static Attachment rs2Attachment(final ResultSet rs) throws SQLException {
    return new Attachment()
        .setId(rs.getLong(Column.CommentFile.ID))
        .setName(rs.getString(Column.CommentFile.NAME))
        .setName(rs.getString(Column.CommentFile.NOTES));
  }
}
