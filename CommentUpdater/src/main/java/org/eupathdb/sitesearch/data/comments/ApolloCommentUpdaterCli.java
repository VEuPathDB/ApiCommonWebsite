package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class ApolloCommentUpdaterCli extends CommentUpdaterCli {

  public static void main(String[] args) throws Exception {
    new ApolloCommentUpdaterCli().execute();
  }

  @Override
  protected CommentUpdater createCommentUpdater(Config config, DatabaseInstance commentDb) {
    return new ApolloCommentUpdater(config.getSolrUrl(), commentDb, config.getCommentSchema());
  }

  @Override
  protected String getEnvDbConnect() {
    return "APPDB_CONNECT";
  }

  @Override
  protected String getEnvDbUser() {
    return "APPDB_LOGIN";
  }

  @Override
  protected String getEnvDbPass() {
    return "APPDB_PASSWORD";
  }

  @Override
  protected String getEnvDbSchema() {
    return "APPDB_SCHEMA";
  }
}
