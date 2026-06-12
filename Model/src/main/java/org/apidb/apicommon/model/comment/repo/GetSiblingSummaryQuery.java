package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.VARCHAR;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apidb.apicommon.model.comment.pojo.SiblingSummary;

/**
 * Aggregate the {@code comment_ai_provenance} rows for one {@code run_job_id}
 * into the anonymous {@link SiblingSummary} ({@code reviewed} / {@code edited}
 * counts + the most recent publish time). One row of counts always comes back,
 * so a run with no published siblings yields {@code (0, 0, null)}.
 *
 * <p>Uses {@code COUNT(CASE WHEN …)} rather than the PostgreSQL-only
 * {@code FILTER} clause so the query stays portable.
 */
public class GetSiblingSummaryQuery extends ValueQuery<SiblingSummary> {

  private static final String SQL = "SELECT" +
      " COUNT(CASE WHEN NOT is_edited THEN 1 END) AS reviewed," +
      " COUNT(CASE WHEN is_edited     THEN 1 END) AS edited," +
      " MAX(created_at) AS latest_at" +
      " FROM %s." + Table.COMMENT_AI_PROVENANCE +
      " WHERE run_job_id = ?";

  private static final Integer[] TYPES = { VARCHAR };

  private final String _runJobId;

  public GetSiblingSummaryQuery(String schema, String runJobId) {
    super(schema);
    _runJobId = runJobId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected SiblingSummary parseResults(ResultSet rs) throws SQLException {
    if (!rs.next())
      return new SiblingSummary(0, 0, null);
    int reviewed = rs.getInt("reviewed");
    int edited = rs.getInt("edited");
    Timestamp latest = rs.getTimestamp("latest_at");
    return new SiblingSummary(reviewed, edited, latest);
  }

  @Override
  protected Object[] getParams() {
    return new Object[] { _runJobId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }
}
