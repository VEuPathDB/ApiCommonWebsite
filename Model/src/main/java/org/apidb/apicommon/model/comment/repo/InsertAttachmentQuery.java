package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.pojo.Attachment;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

/**
 * Insert a link between a comment and a user file.
 */
public class InsertAttachmentQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s.COMMENTFILE (" +
      "FILE_ID, NAME, NOTES, COMMENT_ID) VALUES (?, ?, ?, ?)";

  private static final Integer[] TYPES = { BIGINT, VARCHAR, VARCHAR, BIGINT };

  private final long _comId;

  private final Attachment _attachment;

  public InsertAttachmentQuery(String schema, long commentId, Attachment att) {
    super(schema, Table.COMMENT_TO_CATEGORY, null);
    _comId = commentId;
    _attachment = att;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() {
    final BasicArgumentBatch batch = new BasicArgumentBatch();

    batch.add(new Object[]{_attachment.getId(), _attachment.getName(),
        _attachment.getDescription(), _comId });
    batch.setParameterTypes(TYPES);

    return batch;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
