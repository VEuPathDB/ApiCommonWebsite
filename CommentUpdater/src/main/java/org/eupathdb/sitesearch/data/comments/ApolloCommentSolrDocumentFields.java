package org.eupathdb.sitesearch.data.comments;

public class ApolloCommentSolrDocumentFields extends CommentSolrDocumentFields {

  @Override
  String getCommentIdFieldName() {
    return "apolloCommentIds";
  }

  @Override
  String getCommentContentFieldName() {
    return "MULTITEXT__gene_ApolloCommentContent";
  }

}
