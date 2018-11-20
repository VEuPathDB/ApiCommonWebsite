package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.ReferenceType;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.sql.SQLException;
import java.util.Collection;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

/**
 * Insert a link between a comment and records in a generic
 * data source.
 */
public class InsertReferencesQuery extends InsertQuery {

  private static final String QUERY = "INSERT INTO %s.COMMENTREFERENCE (" +
      "COMMENT_REFERENCE_ID, SOURCE_ID, DATABASE_NAME, COMMENT_ID) VALUES" +
      "(?, ?, ?, ?)";

  private static final Integer[] TYPES = { BIGINT, VARCHAR, VARCHAR, BIGINT };

  private final long _commentId;

  private ReferenceType _type;

  private Collection<String> _ids;

  public InsertReferencesQuery(String schema, long commentId,
      IdSupplier idSupplier) {
    super(schema, Table.COMMENT_TO_REFERENCE, idSupplier);
    _commentId = commentId;
  }

  public InsertReferencesQuery load(ReferenceType type, Collection<String> ids) {
    _type = type;
    _ids  = ids;

    return this;
  }

  @Override
  protected String getQuery() {
    return QUERY;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() throws SQLException {
    final BasicArgumentBatch batch = new BasicArgumentBatch();

    for (String id : _ids)
      batch.add(new Object[] { nextId(), id, _type.dbName, _commentId });

    batch.setParameterTypes(TYPES);

    return batch;
  }
}
