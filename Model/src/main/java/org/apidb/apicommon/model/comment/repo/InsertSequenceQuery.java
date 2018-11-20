package org.apidb.apicommon.model.comment.repo;

import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.io.StringReader;
import java.sql.SQLException;

import static java.sql.Types.BIGINT;
import static java.sql.Types.CLOB;

/**
 * Insert sequence for a comment.
 */
public class InsertSequenceQuery extends InsertQuery {
  private static final String QUERY = "INSERT INTO %s.COMMENTSEQUENCE(" +
      "COMMENT_SEQUENCE_ID, SEQUENCE, COMMENT_ID) VALUES (?,?,?)";

  private static final Integer[] TYPES = {BIGINT, CLOB, BIGINT};
  private final String _sequence;
  private final long _comId;

  public InsertSequenceQuery(String schema, String sequence, long comId,
      IdSupplier idProvider) {
    super(schema, Table.COMMENT_TO_SEQUENCE, idProvider);
    _sequence = sequence;
    _comId = comId;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch out = new BasicArgumentBatch();
    out.add(new Object[]{ nextId(), new StringReader(_sequence), _comId });
    out.setParameterTypes(TYPES);
    return out;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }
}
