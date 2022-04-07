package org.eupathdb.sitesearch.data.comments;

public class ApolloCommentUpdaterCli extends CommentUpdaterCli {

  public ApolloCommentUpdaterCli(CommentSolrDocumentFields docFields, CommentUpdaterSql updaterSql) {
    super(docFields, updaterSql);
  }

  public static void main(String[] args) throws Exception {
    ApolloCommentUpdaterCli cli = 
        new ApolloCommentUpdaterCli(new ApolloCommentSolrDocumentFields(), new ApolloCommentUpdaterSql());
    cli.execute();
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
