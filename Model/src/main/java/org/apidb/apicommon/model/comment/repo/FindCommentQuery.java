package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.*;
import org.apidb.apicommon.model.comment.pojo.Comment;
import org.apidb.apicommon.model.comment.repo.Column.*;
import org.apidb.apicommon.model.comment.repo.Column.ExternalDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

/**
 * Find comments by one or both of the following filters:
 *
 * * comment author
 * * comment target type & comment target id
 */
public class FindCommentQuery extends BaseCommentQuery<Collection<Comment>> {

  private static final String QUERY = "WITH ids AS (\n" +
    "  SELECT\n" +
    "    a." + Column.Comment.ID + "\n" +
    "  FROM\n" +
    "    %1$s." + Table.COMMENTS + " a\n" +
    "    LEFT JOIN %1$s." + Table.COMMENT_TO_STABLE_ID + " b\n" +
    "      ON a." + Column.Comment.ID + " = b." + CommentStableId.COMMENT_ID + "\n" +
    "  WHERE\n" +
    "    %2$s\n" +
    "    AND a." + Column.Comment.VISIBLE + " = 1\n" +
    ")\n" +
    "SELECT\n" +
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
    "  ids\n" +
    "  INNER JOIN %1$s." + Table.COMMENTS + " co\n" +
    "    ON ids.COMMENT_ID = co." + Column.Comment.ID + "\n" +
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
    "    ON coca." + CommentCategory.CATEGORY_ID + " = ca." + TargetCategory.ID + "\n";
  private static final String USER_FILTER = "a." + Column.Comment.USER_ID + " = ?";
  private static final String TARGET_FILTER = "a." + Column.Comment.TARGET_ID + " = ?\n" +
      "    AND ? IN (a." + Column.Comment.STABLE_ID + ", b." + CommentStableId.STABLE_ID + ")";

  private String filter;

  private Object[] params;

  private Integer[] types;

  public FindCommentQuery(String schema) {
    super(schema);
  }

  public FindCommentQuery setFilter(long authorId, String type, String stableId) {
    filter = USER_FILTER + "AND " + TARGET_FILTER;
    params = new Object[] { authorId, type, stableId };
    types = new Integer[] { BIGINT, VARCHAR, VARCHAR };

    return this;
  }

  public FindCommentQuery setFilter(String type, String stableId) {
    filter = TARGET_FILTER;
    params = new Object[] { type, stableId };
    types = new Integer[] { VARCHAR, VARCHAR };

    return this;
  }

  public FindCommentQuery setFilter(long authorId) {
    filter = USER_FILTER;
    params = new Object[] { authorId };
    types = new Integer[] { BIGINT };

    return this;
  }

  @Override
  protected String getQuery() {
    return String.format(QUERY, "%1$s", filter);
  }

  @Override
  protected Object[] getParams() {
    return params;
  }

  @Override
  protected Integer[] getTypes() {
    return types;
  }

  @Override
  protected Collection<Comment> parseResults(ResultSet rs) throws SQLException {
    final Map<Long, Comment> out = new LinkedHashMap<>();

    while(rs.next()) {
      final Comment com;
      final long id = rs.getLong(Column.Comment.ID);

      if(!out.containsKey(id)) {
        com = rs2Comment(rs);
        out.put(id, com);
      } else {
        com = out.get(id);
      }

      if(com.getSequence() == null)
        rs2Sequence(rs).ifPresent(com::setSequence);

      if(com.getLocation() == null)
        rs2Location(rs).ifPresent(com::setLocation);

      if(com.getExternalDatabase() == null)
        rs2ExternalDb(rs).ifPresent(com::setExternalDatabase);

      rs2LocationRange(rs).ifPresent(
          rng -> com.locationOption().ifPresent(loc -> loc.addRange(rng)));
      rs2Related(rs).ifPresent(com::addRelatedStableId);
      rs2Reference(rs).ifPresent(t -> appendReference(com, t));
      rs2Attachment(rs).ifPresent(com::addAttachment);
      rs2Category(rs).map(Category::getName).ifPresent(com::addCategory);
    }

    return out.values();
  }
}
