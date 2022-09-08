package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class ApolloCommentUpdaterCli extends CommentUpdaterCli {

  public static void main(String[] args) throws Exception {
    new ApolloCommentUpdaterCli().execute();
  }

  @Override
  protected CommentUpdater<String> createCommentUpdater(Config config, DatabaseInstance commentDb) {
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
    // unlike user comments, apollo comments do not need a schema, nor a schema env var.  as a hack reuse an existing env var.  this will pass validation but is not ever used.
    // @TODO factor this propertly in the super class to avoid this hack
    return "APPDB_LOGIN"; 
  }
}
