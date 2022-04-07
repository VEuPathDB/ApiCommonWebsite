package org.eupathdb.sitesearch.data.comments;

public interface CommentUpdaterSql {
  String getGeneIdWithCommentIdSql(String schema); 
  String getSortedCommentsSql(String schema);
}
