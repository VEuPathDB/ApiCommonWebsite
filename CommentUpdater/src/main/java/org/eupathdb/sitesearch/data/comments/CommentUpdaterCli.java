package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public abstract class CommentUpdaterCli {

  // subclass to provide implementation-specific comment updater
  protected abstract CommentUpdater createCommentUpdater(Config config, DatabaseInstance commentDb);

  // subclass to provide env vars used to configure DB
  protected abstract String getEnvDbConnect();
  protected abstract String getEnvDbUser();
  protected abstract String getEnvDbPass();
  protected abstract String getEnvDbSchema();

  private static String getEnvSolrUrl() { return "SOLR_URL"; }

  private String getBadEnvMsg() {
    return "Comment updater requires the following environment variables:\n"
      + "    " + getEnvDbConnect() + ": Database connection string\n"
      + "    " + getEnvDbUser()    + ": Database credentials username\n"
      + "    " + getEnvDbPass()    + ": Database credentials password\n"
      + "    " + getEnvDbSchema()  + ": Database comment schema (not needed for Apollo Comment Updater)\n"
      + "    " + getEnvSolrUrl()   + ": Solr URL"; 
  }

  protected static class Config extends ThreeTuple<String,ConnectionPoolConfig,String> {
    Config(String solrUrl, ConnectionPoolConfig commentDbConfig, String commentDbSchema) {
      super(solrUrl, commentDbConfig, commentDbSchema);
    }
    String getSolrUrl() { return getFirst(); }
    ConnectionPoolConfig getDbConfig() { return getSecond(); }
    String getCommentSchema() { return getThird(); }
  }

  void execute() throws Exception {
    Config config = parseEnv();
    try (DatabaseInstance commentDb = new DatabaseInstance(config.getDbConfig())) {
      createCommentUpdater(config, commentDb).syncAll();
    }
  }

  private Config parseEnv() {
    final var env = System.getenv();

    if (!(env.containsKey(getEnvDbConnect()) && env.containsKey(getEnvDbPass())
        && env.containsKey(getEnvDbSchema()) && env.containsKey(getEnvDbUser())
        && env.containsKey(getEnvSolrUrl()))
    ) {
      System.err.println(getBadEnvMsg());
      System.exit(1);
    }

    final var schema = env.get(getEnvDbSchema());
    return new Config(
      env.get(getEnvSolrUrl()),
      SimpleDbConfig.create(
        SupportedPlatform.ORACLE,
        env.get(getEnvDbConnect()),
        env.get(getEnvDbUser()),
        env.get(getEnvDbPass())
      ),
      schema.endsWith(".") ? schema : schema + "."
    );
  }

}
