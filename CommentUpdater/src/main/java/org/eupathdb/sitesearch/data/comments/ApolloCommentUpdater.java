package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class ApolloCommentUpdater extends CommentUpdater {

  private static class ApolloCommentSolrDocumentFields extends CommentSolrDocumentFields {

    @Override
    String getCommentIdFieldName() {
      return "apolloCommentIds";
    }

    @Override
    String getCommentContentFieldName() {
      return "MULTITEXT__gene_ApolloCommentContent";
    }

  }

  private static class ApolloCommentUpdaterSql implements CommentUpdaterSql {

    @Override
    public String getGeneIdWithCommentIdSql(String schema) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getSortedCommentsSql(String schema) {
      // TODO Auto-generated method stub
      return null;
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

}
