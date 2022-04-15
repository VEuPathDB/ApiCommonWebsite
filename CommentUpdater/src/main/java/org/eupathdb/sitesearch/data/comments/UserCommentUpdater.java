package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class UserCommentUpdater extends CommentUpdater {

  private static class UserCommentSolrDocumentFields extends CommentSolrDocumentFields {

    @Override
    String getCommentIdFieldName() {
      return "userCommentIds";
    }

    @Override
    String getCommentContentFieldName() {
      return "MULTITEXT__gene_UserCommentContent";
    }

  }

  private static class UserCommentUpdaterSql implements CommentUpdaterSql {

    @Override
    public String getGeneIdWithCommentIdSql(String schema) {
      return "SELECT stable_id "
          + "FROM " + schema + "comments "
          + "WHERE comment_id = ?"
          + "UNION "
          + "SELECT stable_id "
          + "FROM " + schema + "commentstableid "
          + "WHERE comment_id = ?";
    }

    @Override
    public String getSortedCommentsSql(String schema) {
      return "SELECT source_id, comment_target_type as record_type, c.comment_id"
          + " FROM apidb.textsearchablecomment tsc,"
          + " " + schema + "comments c "
          + " WHERE tsc.comment_id = c.comment_id"
          + " ORDER BY source_id DESC, c.comment_id";
    }

  }

  public UserCommentUpdater(
      String solrUrl,
      DatabaseInstance commentDb,
      String commentSchema) {
    super(solrUrl, commentDb, commentSchema,
        new UserCommentSolrDocumentFields(),
        new UserCommentUpdaterSql());
  }

}
