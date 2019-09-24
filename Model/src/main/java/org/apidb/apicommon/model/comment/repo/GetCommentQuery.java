package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Category;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.repo.Column.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.BIGINT;

/**
 * Get a single comment by id if that comment exists.
 */
public class GetCommentQuery extends BaseCommentQuery<Optional<Comment>> {

  private static final String SQL = "SELECT\n" +
    "  co." + Column.Comment.ID               + ",\n" +
    "  co." + Column.Comment.USER_ID          + ",\n" +
    "  co." + Column.Comment.DATE             + ",\n" +
    "  co." + Column.Comment.TARGET_ID        + ",\n" +
    "  co." + Column.Comment.STABLE_ID        + ",\n" +
    "  co." + Column.Comment.CONCEPTUAL       + ",\n" +
    "  co." + Column.Comment.PROJECT_NAME     + ",\n" +
    "  co." + Column.Comment.PROJECT_VERSION  + ",\n" +
    "  co." + Column.Comment.HEADLINE         + ",\n" +
    "  co." + Column.Comment.LOCATION         + ",\n" +
    "  co." + Column.Comment.CONTENT          + ",\n" +
    "  co." + Column.Comment.ORGANISM         + ",\n" +
    "  co." + Column.Comment.REVIEW_STATUS    + ",\n" +
    "  us." + CommentUser.FIRST_NAME          + ",\n" +
    "  us." + CommentUser.LAST_NAME           + ",\n" +
    "  us." + CommentUser.ORGANIZATION        + ",\n" +
    "  id." + CommentStableId.STABLE_ID       + " " + RELATED + ",\n" +
    "  se." + CommentSequence.SEQUENCE        + ",\n" +
    "  db." + Column.ExternalDatabase.NAME    + ",\n" +
    "  db." + Column.ExternalDatabase.VERSION + ",\n" +
    "  fi." + CommentFile.ID                  + ",\n" +
    "  fi." + CommentFile.NAME                + ",\n" +
    "  fi." + CommentFile.NOTES               + ",\n" +
    "  re." + CommentReference.SOURCE_ID      + ",\n" +
    "  re." + CommentReference.DATABASE       + ",\n" +
    "  lo." + Column.Location.REVERSED        + ",\n" +
    "  lo." + Column.Location.COORD_TYPE      + ",\n" +
    "  lo." + Column.Location.START           + ",\n" +
    "  lo." + Column.Location.END             + ",\n" +
    "  ca." + TargetCategory.NAME             + ",\n" +
    "  ca." + TargetCategory.ID               + "\n" +
    "FROM\n" +
    "%1$s." + Table.COMMENTS + " co\n" +
    "  INNER JOIN %1$s." + Table.COMMENT_USERS + " us\n" +
    "    ON co." + Column.Comment.USER_ID + " = us." + CommentUser.ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_STABLE_ID + " id\n" +
    "    ON co." + Column.Comment.ID + " = id." + CommentStableId.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_SEQUENCE + " se\n" +
    "    ON co." + Column.Comment.ID + " = se." + CommentSequence.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_EXT_DB + " codb\n" +
    "    ON co." + Column.Comment.ID + " = codb." + CommentExternalDb.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_FILE + " fi\n" +
    "    ON co." + Column.Comment.ID + " = fi." + CommentFile.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_REFERENCE + " re\n" +
    "    ON co." + Column.Comment.ID + " = re." + CommentReference.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.EXTERNAL_DBS + " db\n" +
    "    ON codb." + CommentExternalDb.DB_ID + " = db." + ExternalDatabase.ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_LOCATION + " lo\n" +
    "    ON co." + Column.Comment.ID + " = lo." + Column.Location.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.COMMENT_TO_CATEGORY + " coca\n" +
    "    ON co." + Column.Comment.ID + " = coca." + CommentCategory.COMMENT_ID + "\n" +
    "  LEFT JOIN %1$s." + Table.CATEGORIES + " ca\n" +
    "    ON coca." + CommentCategory.CATEGORY_ID + " = ca." + TargetCategory.ID + "\n" +
    "WHERE\n" +
    "  co.COMMENT_ID = ?";

  private static final Integer[] TYPES = { BIGINT };

  private long _comId;

  public GetCommentQuery(String schema, long id) {
    super(schema);
    _comId = id;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected Optional<Comment> parseResults(ResultSet rs) throws SQLException {
    if (!rs.next())
      return Optional.empty();

    Comment out = rs2Comment(rs);
    rs2Sequence(rs).ifPresent(out::setSequence);
    rs2Location(rs).ifPresent(out::setLocation);
    rs2ExternalDb(rs).ifPresent(out::setExternalDatabase);

    do {
      rs2LocationRange(rs).ifPresent(
          rng -> out.locationOption().ifPresent(loc -> loc.addRange(rng)));
      rs2Related(rs).ifPresent(out::addRelatedStableId);
      rs2Reference(rs).ifPresent(t -> appendReference(out, t));
      rs2Attachment(rs).ifPresent(out::addAttachment);
      rs2Category(rs).map(Category::getName).ifPresent(out::addCategory);
    } while(rs.next());

    out.getRelatedStableIds().remove(out.getTarget().getId());

    return Optional.of(out);
  }

  @Override
  protected Object[] getParams() {
    return new Object[]{ _comId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }
}
