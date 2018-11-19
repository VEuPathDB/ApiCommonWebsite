package org.apidb.apicommon.model.comment.repo;

import org.apidb.apicommon.model.comment.ReviewStatus;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.apidb.apicommon.model.comment.pojo.Project;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.user.User;

import java.io.StringReader;

import static java.sql.Types.*;

public class InsertCommentQuery extends InsertQuery {
  private static final String SQL =
      "INSERT INTO %s.COMMENTS (\n" + "  COMMENT_ID, USER_ID, EMAIL,\n" +
          "  COMMENT_DATE, COMMENT_TARGET_ID, STABLE_ID,\n" +
          "  CONCEPTUAL, PROJECT_NAME, PROJECT_VERSION,\n" +
          "  HEADLINE, REVIEW_STATUS_ID, CONTENT,\n" +
          "  ORGANISM, PREV_COMMENT_ID\n" + ")\n" +
          "VALUES (?, ?, ?, current_timestamp, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?)";

  private static final Integer[] TYPES = {
      BIGINT,  // COMMENT_ID
      BIGINT,  // USER_ID
      VARCHAR, // EMAIL
      VARCHAR, // COMMENT_TARGET_ID
      VARCHAR, // STABLE_ID
      VARCHAR, // PROJECT_NAME
      VARCHAR, // PROJECT_VERSION
      VARCHAR, // HEADLINE
      VARCHAR, // REVIEW_STATUS_ID
      CLOB,    // CONTENT
      VARCHAR, // ORGANISM
      BIGINT   // PREV_COMMENT_ID
  };

  private final CommentRequest _req;
  private final long _id;
  private final User _user;
  private final Project _proj;

  public InsertCommentQuery(String schema, long id, CommentRequest req,
      User user, Project proj) {
    super(schema, Table.COMMENTS, null);
    _req = req;
    _id = id;
    _user = user;
    _proj = proj;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected SQLRunner.ArgumentBatch getArguments() {
    final BasicArgumentBatch out = new BasicArgumentBatch();

    out.add(new Object[] {
        _id,                                 // COMMENT_ID
        _req.getUserId(),                    // USER_ID
        _user.getEmail(),                    // EMAIL
        _req.getTarget().getType(),          // COMMENT_TARGET_ID
        _req.getTarget().getId(),            // STABLE_ID
        _proj.getName(),                     // PROJECT_NAME
        _proj.getVersion(),                  // PROJECT_VERSION
        _req.getHeadline(),                  // HEADLINE
        ReviewStatus.UNKNOWN.dbName,         // REVIEW_STATUS_ID
        new StringReader(_req.getContent()), // CONTENT
        _req.getOrganism(),                  // ORGANISM
        _req.getPreviousCommentId()          // PREV_COMMENT_ID
    });

    out.setParameterTypes(TYPES);

    return out;
  }
}
