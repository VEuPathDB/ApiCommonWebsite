package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.WOT;
import org.apidb.apicommon.model.comment.pojo.Attachment;
import org.gusdb.wdk.model.WdkModelException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class AttachmentRepository extends Repository {

  final class Column {
    private static final String
        FILE_ID = "file_id",
        NAME    = "name",
        NOTES   = "notes";
  }

  private static final String TABLE_NAME = "COMMENTFILE";

  private static final String SELECT_DETAILS = "SELECT *\n" +
      "FROM %s.COMMENTFILE\n" +
      "WHERE COMMENT_ID = ?";

  private static final String SELECT_COUNT = "SELECT COUNT(1)\n" +
      "FROM %s.COMMENTFILE\n" +
      "WHERE COMMENT_ID = ?";

  public AttachmentRepository(String schema, DataSource ds,
      InsertQuery.IdSupplier<String, Long> seqNext) {

    super(schema, TABLE_NAME, ds, seqNext);
  }

  /**
   * Fetch attachment details for a comment
   *
   * @param commentId ID of the comment for which to lookup attachments
   *
   * @return collection of files attached to the given comment.
   */
  public Collection<Attachment> getByComment(long commentId)
      throws WdkModelException {
    WOT.START();

    final Collection<Attachment> out = new ArrayList<>();

    try(
      Connection con = connect();
      PreparedStatement ps = prepare(con, SELECT_DETAILS)
    ) {
      ps.setLong(1, commentId);

      try(ResultSet rs = ps.executeQuery()) {
        while(rs.next())
          out.add(rs2Attachment(rs));
      }
    } catch (SQLException e) {
      throw new WdkModelException(e);
    }

    WOT.STOP();
    return out;
  }

  private Attachment rs2Attachment(final ResultSet rs) throws SQLException {
    return new Attachment()
        .setId(rs.getLong(Column.FILE_ID))
        .setName(rs.getString(Column.NAME))
        .setDescription(rs.getString(Column.NOTES));
  }
}
