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
      return "select source_id from apidbTuning.ApolloSiteSearch where id_attr = ?";
    }

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

}
