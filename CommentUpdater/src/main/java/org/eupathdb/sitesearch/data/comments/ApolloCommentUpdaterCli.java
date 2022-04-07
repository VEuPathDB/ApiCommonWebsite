package org.eupathdb.sitesearch.data.comments;

public class ApolloCommentUpdaterCli extends CommentUpdaterCli {

  public static void main(String[] args) throws Exception {
    UserCommentUpdaterCli cli = new UserCommentUpdaterCli();
    cli.init();
  }
  
  @Override
  String getEnvDbConnect() {
    return "APPDB_CONNECT";
  }

  @Override
  String getEnvDbUser() {
    return "APPDB_LOGIN";
  }

  @Override
  String getEnvDbPass() {
    return "APPDB_PASSWORD";
  }

  @Override
  String getEnvDbSchema() {
    return "APPDB_SCHEMA";
  }
}
