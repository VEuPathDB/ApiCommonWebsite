package org.eupathdb.sitesearch.data.comments;

import java.sql.Types;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.eupathdb.sitesearch.data.comments.solr.SolrUrlQueryBuilder;

public class UserCommentUpdater extends CommentUpdater<Integer> {

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
    public String getSortedCommentsSql(String schema) {
      return "SELECT stable_id, comment_target_id as record_type, comment_id"
          + " FROM " + schema + "comments c "
          + " AND comment_target_id = 'gene'"  // until comment updater can handle other record classes
          + " AND is_visible = true"
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
  
   @Override
   SolrUrlQueryBuilder applyOptionalSolrFilters(SolrUrlQueryBuilder builder) {
     return builder;
   }

  /**
   * Get the up-to-date comments info from the database, for the provided wdk
   * record
   */
  @Override
  DocumentCommentsInfo<Integer> getCorrectCommentsForOneSourceId(
    final String sourceId,
    final DataSource commentDbDataSource,
    final String commentSchema
  ) {

    var sqlSelect = "select c.comment_id," 
      + " c.headline || '|' || c.content || '|' || u.first_name || ' ' || u.last_name || '(' || u.organization || ')'  || '|' || a.authors as content"
      + " from " + commentSchema + "comments c,"
      + " " + commentSchema + "comment_users u,"
      + " (select comment_id, string_agg(source_id , ', ' order by source_id) authors"
      + "  from " + commentSchema + "CommentReference"
      + "  where database_name = 'author'"
      + "  group by comment_id) a"
      + " where u.user_id = c.user_id"
      + " and c.comment_id = a.comment_id"
      + " and c.stable_id = '" + sourceId + "'";

    return new SQLRunner(commentDbDataSource, sqlSelect)
      .executeQuery(rs -> {
        var comments = new DocumentCommentsInfo<Integer>();

        while (rs.next()) {
          comments.commentIds.add(rs.getInt("comment_id"));
          comments.commentContents.add(rs.getString("content"));
        }

        return comments;
      });
  }

  /*
   * called when user edits a comment 
   */
  public void updateSingle(final long commentId) {
    // Intentionally selecting dead comments for comment delete case.
    var select = getGeneIdWithCommentIdSql(getCommentSchema());
    var genes  = new SQLRunner(getCommentDb().getDataSource(), select)
      .executeQuery(
        new Object[] {commentId, commentId},
        new Integer[] {Types.BIGINT, Types.BIGINT},
        rs -> {
          var tmp = new ArrayList<String>();
          while (rs.next()) {
            tmp.add("gene__" + rs.getString(1));
          }
          return tmp.toArray(new String[0]);
        });
    try {
      fetchDocumentsById(genes).values().forEach(this::updateDocumentComment);
    } finally {
      solrCommit();
    }
  }
  
  private String getGeneIdWithCommentIdSql(String schema) {
    return "SELECT stable_id "
        + "FROM " + schema + "comments "
        + "WHERE comment_id = ? "
        + "UNION "
        + "SELECT stable_id "
        + "FROM " + schema + "commentstableid "
        + "WHERE comment_id = ?";
  }



}
