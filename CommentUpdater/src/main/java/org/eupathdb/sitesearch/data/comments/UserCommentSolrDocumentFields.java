package org.eupathdb.sitesearch.data.comments;

public class UserCommentSolrDocumentFields extends CommentSolrDocumentFields {

  @Override
  String getCommentIdFieldName() {
    return "userCommentIds";
  }

  @Override
  String getCommentContentFieldName() {
    return "MULTITEXT__gene_UserCommentContent";
  }

}
