package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public abstract class CommentUpdaterCli {
  
  abstract String getEnvDbConnect();
  abstract String getEnvDbUser();
  abstract String getEnvDbPass();
  abstract String getEnvDbSchema();
  
  private static String getEnvSolrUrl() { return "SOLR_URL"; }

  private String getBadEnvMsg() {
    return "Comment updater requires the following environment variables:\n"
      + "    " + getEnvDbConnect() + ": User database connection string\n"
      + "    " + getEnvDbUser()    + ": User database credentials username\n"
      + "    " + getEnvDbPass()    + ": User database credentials password\n"
      + "    " + getEnvDbSchema()  + ": User database comment schema\n"
      + "    " + getEnvSolrUrl()   + ": Solr URL"; 
  }

  private static class Config extends ThreeTuple<String,ConnectionPoolConfig,String> {
    Config(String solrUrl, ConnectionPoolConfig commentDbConfig, String commentDbSchema) {
      super(solrUrl, commentDbConfig, commentDbSchema);
    }
    String getSolrUrl() { return getFirst(); }
    ConnectionPoolConfig getDbConfig() { return getSecond(); }
    String getCommentSchema() { return getThird(); }
  }

  
  void init() throws Exception {
    Config config = parseEnv();
    try (DatabaseInstance commentDb = new DatabaseInstance(config.getDbConfig())) {
      CommentUpdater updater = new CommentUpdater(config.getSolrUrl(), commentDb, config.getCommentSchema());
      updater.syncAll();
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
