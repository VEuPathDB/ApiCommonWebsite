package org.eupathdb.sitesearch.data.comments;

public class ApolloCommentUpdaterSql implements CommentUpdaterSql
{
  @Override
  public String getGeneIdWithCommentIdSql(String schema) {
    // TODO Auto-generated method stub
    return "select source_id from apidbTuning.ApolloSiteSearch where id_attr = ?";
  }

  @Override
  public String getSortedCommentsSql(String schema) {
    return "SELECT source_id, 'gene' as record_type, id_attr"
    + " FROM apidbTuning.ApolloSiteSearch"
    + " ORDER BY source_id DESC, id_attr";
  }

}
