package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Location;
import org.apidb.apicommon.model.comment.pojo.LocationRange;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.SQLException;

import static java.sql.Types.BIGINT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;
import static java.sql.Types.BOOLEAN;

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
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch batch = new BasicArgumentBatch();

    for (LocationRange range : _locs.getRanges()) {
      batch.add(
          new Object[] { _comId, nextId(), range.getStart(), range.getEnd(),
              _locs.getCoordinateType(), _locs.isReverse()});
    }

    batch.setParameterTypes(TYPES);

    return batch;
  }
}
