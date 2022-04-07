package org.eupathdb.sitesearch.data.comments;

public class UserCommentUpdaterCli extends CommentUpdaterCli {
  
  public static void main(String[] args) throws Exception {
    UserCommentUpdaterCli cli = new UserCommentUpdaterCli();
    cli.init(new UserCommentSolrDocumentFields(), new UserCommentUpdaterSql());
  }
  
  @Override
  String getEnvDbConnect() {
    return "USERDB_CONNECT";
  }

  @Override
  String getEnvDbUser() {
    return "USERDB_LOGIN";
  }

  @Override
  String getEnvDbPass() {
    return "USERDB_PASSWORD";
  }

  @Override
  String getEnvDbSchema() {
    return "USERDB_SCHEMA";
  }

}
