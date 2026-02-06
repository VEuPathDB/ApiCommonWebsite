package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.BIGINT;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.VARCHAR;

import java.sql.SQLException;

import org.apidb.apicommon.model.comment.pojo.Location;
import org.apidb.apicommon.model.comment.pojo.LocationRange;
import org.gusdb.fgputil.db.runner.ArgumentBatch;
import org.gusdb.fgputil.db.runner.ListArgumentBatch;

/**
 * Insert new location entries for a comment.
 */
public class InsertLocationQuery extends InsertQuery {

  private static final String QUERY = "INSERT INTO %s.LOCATIONS (" +
      "COMMENT_ID, LOCATION_ID, LOCATION_START, LOCATION_END, COORDINATE_TYPE," +
      "IS_REVERSE) VALUES (?, ?, ?, ?, ?, ?)";

  private static final Integer[] TYPES = {
      BIGINT,  // COMMENT_ID
      BIGINT,  // LOCATION_ID
      BIGINT,  // LOCATION_START
      BIGINT,  // LOCATION_END
      VARCHAR, // COORDINATE_TYPE
      BOOLEAN  // IS_REVERSE
  };

  private final long _comId;

  private final Location _locs;

  public InsertLocationQuery(String schema, long commentId, Location locs,
      IdSupplier idProvider) {
    super(schema, Table.COMMENT_TO_LOCATION, idProvider);
    _comId = commentId;
    _locs = locs;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }

  @Override
  protected ArgumentBatch getArguments() throws SQLException {
    final ListArgumentBatch batch = new ListArgumentBatch();

    for (LocationRange range : _locs.getRanges()) {
      batch.add(
          new Object[] { _comId, nextId(), range.getStart(), range.getEnd(),
              _locs.getCoordinateType(), _locs.isReverse()});
    }

    batch.setParameterTypes(TYPES);

    return batch;
  }
}
