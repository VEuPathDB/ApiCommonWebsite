package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class UserCommentUpdaterCli extends CommentUpdaterCli {
  
  public static void main(String[] args) throws Exception {
    new UserCommentUpdaterCli().execute();
  }

  @Override
  protected CommentUpdater createCommentUpdater(Config config, DatabaseInstance commentDb) {
    return new UserCommentUpdater(config.getSolrUrl(), commentDb, config.getCommentSchema());
  }
  
  @Override
  protected String getEnvDbConnect() {
    return "USERDB_CONNECT";
  }

  @Override
  protected String getEnvDbUser() {
    return "USERDB_LOGIN";
  }

  @Override
  protected String getEnvDbPass() {
    return "USERDB_PASSWORD";
  }

  @Override
  protected String getEnvDbSchema() {
    return "USERDB_SCHEMA";
  }

}
