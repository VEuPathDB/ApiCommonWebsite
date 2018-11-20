package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Comment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static java.sql.Types.BIGINT;

/**
 * Get a single comment by id if that comment exists.
 */
public class GetCommentQuery extends BaseCommentQuery<Optional<Comment>> {

  private static final String SQL = "SELECT\n" + "  co.COMMENT_ID,\n" +
    "  co.USER_ID,\n" + "  co.COMMENT_DATE,\n" + "  co.COMMENT_TARGET_ID,\n" +
    "  co.STABLE_ID,\n" + "  co.CONCEPTUAL,\n" + "  co.PROJECT_NAME,\n" +
    "  co.PROJECT_VERSION,\n" + "  co.HEADLINE,\n" + "  co.LOCATION_STRING,\n" +
    "  co.CONTENT,\n" + "  co.ORGANISM,\n" + "  co.REVIEW_STATUS_ID,\n" +
    "  co.IS_VISIBLE,\n" + "  cous.FIRST_NAME,\n" + "  cous.LAST_NAME,\n" +
    "  cous.ORGANIZATION,\n" + "  costid.STABLE_ID,\n" + "  cose.SEQUENCE,\n" +
    "  exda.EXTERNAL_DATABASE_NAME,\n" + "  exda.EXTERNAL_DATABASE_VERSION,\n" +
    "  cofi.FILE_ID,\n" + "  cofi.NAME,\n" + "  cofi.NOTES,\n" +
    "  core.SOURCE_ID,\n" + "  core.DATABASE_NAME,\n" + "  lo.IS_REVERSE,\n" +
    "  lo.COORDINATE_TYPE,\n" + "  lo.LOCATION_START,\n" +
    "  lo.LOCATION_END\n" +
    "FROM\n" + "  %1$s.COMMENTS co\n" +
    "  INNER JOIN %1$s.COMMENT_USERS cous\n" +
    "    ON co.USER_ID = cous.USER_ID\n" +
    "  LEFT JOIN %1$s.COMMENTSTABLEID costid\n" +
    "    ON co.COMMENT_ID = costid.COMMENT_ID\n" +
    "  LEFT JOIN %1$s.COMMENTSEQUENCE cose\n" +
    "    ON co.COMMENT_ID = cose.COMMENT_ID\n" +
    "  LEFT JOIN %1$s.COMMENT_EXTERNAL_DATABASE coexda\n" +
    "    ON co.COMMENT_ID = coexda.COMMENT_ID\n" +
    "  LEFT JOIN %1$s.COMMENTFILE cofi\n" +
    "    ON co.COMMENT_ID = cofi.COMMENT_ID\n" +
    "  LEFT JOIN %1$s.COMMENTREFERENCE core\n" +
    "    ON co.COMMENT_ID = core.COMMENT_ID\n" +
    "  LEFT JOIN %1$s.EXTERNAL_DATABASES exda\n" +
    "    ON coexda.EXTERNAL_DATABASE_ID = exda.EXTERNAL_DATABASE_ID\n" +
    "  LEFT JOIN %1$s.LOCATIONS lo\n" +
    "    ON co.COMMENT_ID = lo.COMMENT_ID\n" + "WHERE\n" + "  co.COMMENT_ID = ?";

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
    rs2Location(rs).ifPresent(out::setLocations);
    rs2ExternalDb(rs).ifPresent(out::setExternalDb);

    do {
      rs2LocationRange(rs).ifPresent(out.getLocations()::addRange);
      rs2Related(rs).ifPresent(out::addRelatedStableId);
      rs2Reference(rs).ifPresent(t -> appendReference(out, t));
    } while(rs.next());

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
