package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.*;

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

  private static final String QUERY = "SELECT\n" +
      "  co.COMMENT_ID,\n" +
      "  co.USER_ID,\n" +
      "  co.COMMENT_DATE,\n" +
      "  co.COMMENT_TARGET_ID,\n" +
      "  co.STABLE_ID,\n" +
      "  co.CONCEPTUAL,\n" +
      "  co.PROJECT_NAME,\n" +
      "  co.PROJECT_VERSION,\n" +
      "  co.HEADLINE,\n" +
      "  co.LOCATION_STRING,\n" +
      "  co.CONTENT,\n" +
      "  co.ORGANISM,\n" +
      "  co.REVIEW_STATUS_ID,\n" +
      "  cous.FIRST_NAME,\n" +
      "  cous.LAST_NAME,\n" +
      "  cous.ORGANIZATION,\n" +
      "  costid.STABLE_ID,\n" +
      "  cose.SEQUENCE,\n" +
      "  exda.EXTERNAL_DATABASE_NAME,\n" +
      "  exda.EXTERNAL_DATABASE_VERSION,\n" +
      "  cofi.FILE_ID,\n" +
      "  cofi.NAME,\n" +
      "  cofi.NOTES,\n" +
      "  core.SOURCE_ID,\n" +
      "  core.DATABASE_NAME,\n" +
      "  lo.IS_REVERSE,\n" +
      "  lo.COORDINATE_TYPE,\n" +
      "  lo.LOCATION_START,\n" +
      "  lo.LOCATION_END\n" +
      "FROM\n" +
      "  %1$s.COMMENTS                            co\n" +
      "  INNER JOIN %1$s.COMMENT_USERS            cous\n" +
      "    ON co.USER_ID = cous.USER_ID\n" +
      "  LEFT JOIN %1$s.COMMENTSTABLEID           costid\n" +
      "    ON co.COMMENT_ID = costid.COMMENT_ID\n" +
      "  LEFT JOIN %1$s.COMMENTSEQUENCE           cose\n" +
      "    ON co.COMMENT_ID = cose.COMMENT_ID\n" +
      "  LEFT JOIN %1$s.COMMENT_EXTERNAL_DATABASE coexda\n" +
      "    ON co.COMMENT_ID = coexda.COMMENT_ID\n" +
      "  LEFT JOIN %1$s.COMMENTFILE               cofi\n" +
      "    ON co.COMMENT_ID = cofi.COMMENT_ID\n" +
      "  LEFT JOIN %1$s.COMMENTREFERENCE          core\n" +
      "    ON co.COMMENT_ID = core.COMMENT_ID\n" +
      "  LEFT JOIN %1$s.EXTERNAL_DATABASES        exda\n" +
      "    ON coexda.EXTERNAL_DATABASE_ID = exda.EXTERNAL_DATABASE_ID\n" +
      "  LEFT JOIN %1$s.LOCATIONS                 lo\n" +
      "    ON co.COMMENT_ID = lo.COMMENT_ID\n" +
      "WHERE\n" +
      "  %2$s\n" +
      "  AND co.IS_VISIBLE = 1";
  private static final String USER_FILTER = "co.USER_ID = ?";
  private static final String TARGET_FILTER = "co.COMMENT_TARGET_ID = ?\n" +
      "  AND co.STABLE_ID = ?";

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
    }

    return out.values();
  }
}
