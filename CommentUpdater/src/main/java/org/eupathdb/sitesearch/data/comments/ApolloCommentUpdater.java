package org.eupathdb.sitesearch.data.comments;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;

public class ApolloCommentUpdater extends CommentUpdater<String>{

  private static class ApolloCommentSolrDocumentFields extends CommentSolrDocumentFields {

    @Override
    String getCommentIdFieldName() {
      return "apolloCommentIds";
    }

    @Override
    String getCommentContentFieldName() {
      return "MULTITEXT__gene_apolloCommentContent";
    }

  }

  private static class ApolloCommentUpdaterSql implements CommentUpdaterSql {

    @Override
    public String getSortedCommentsSql(String schema) {
      return "SELECT source_id, 'gene' as record_type, id_attr"
          + " FROM apidbTuning.ApolloSiteSearch"
          + " ORDER BY source_id DESC, id_attr";
    }

  }

  public ApolloCommentUpdater(
      String solrUrl,
      DatabaseInstance commentDb,
      String commentSchema) {
    super(solrUrl, commentDb, commentSchema,
        new ApolloCommentSolrDocumentFields(),
        new ApolloCommentUpdaterSql());
  }
  
  /**
   * Get the up-to-date comments info from the database, for the provided wdk
   * record
   */
  @Override
  DocumentCommentsInfo<String> getCorrectCommentsForOneDocument(
    final SolrDocument doc,
    final DataSource commentDbDataSource
  ) {

    var sqlSelect = 
    " select distinct id_attr as comment_id, apolloproduct, apollosymbol, apolloowner, apollogoterm, apollopmid" + 
    " from  apidbtuning.ApolloUpdate au, ApidbTuning.GeneAttributes ga" +
    " where au.type = 'gene'" +
    " and (au.attr like '%gene_product=%' or au.attr like '%description=%')" +
    " and ga.na_sequence_id = au.na_sequence_id" +
    " and ga.start_min <= au.mapping_end" +
    " and ga.end_max >= au.mapping_start" +
    " and source_id = '"  + doc.getSourceId() + "'";

    return new SQLRunner(commentDbDataSource, sqlSelect)
      .executeQuery(rs -> {
        var comments = new DocumentCommentsInfo<String>();

        while (rs.next()) {
          comments.commentIds.add(rs.getString("comment_id"));
          comments.commentContents.add(rs.getString("apolloproduct"));
          comments.commentContents.add(rs.getString("apollosymbol"));
          comments.commentContents.add(rs.getString("apolloowner"));
          comments.commentContents.add(rs.getString("apollogoterm"));
          comments.commentContents.add(rs.getString("apollopmid"));
       }

        return comments;
      });
  }


}
