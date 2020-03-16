package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public class CommentUpdaterCli {

  private static final String
    ENV_DB_CONNECT = "USERDB_CONNECT",
    ENV_DB_USER    = "USERDB_LOGIN",
    ENV_DB_PASS    = "USERDB_PASSWORD",
    ENV_DB_SCHEMA  = "USERDB_SCHEMA",
    ENV_SOLR_URL   = "SOLR_URL";

  private static final String
    ERR_BAD_ENV = "Comment updater requires the following environment variables:\n"
      + "    " + ENV_DB_CONNECT + ": User database connection string\n"
      + "    " + ENV_DB_USER    + ": User database credentials username\n"
      + "    " + ENV_DB_PASS    + ": User database credentials password\n"
      + "    " + ENV_DB_SCHEMA  + ": User database comment schema\n"
      + "    " + ENV_SOLR_URL   + ": Solr URL";

  private static class Config extends ThreeTuple<String,ConnectionPoolConfig,String> {
    Config(String solrUrl, ConnectionPoolConfig commentDbConfig, String commentDbSchema) {
      super(solrUrl, commentDbConfig, commentDbSchema);
    }
    String getSolrUrl() { return getFirst(); }
    ConnectionPoolConfig getDbConfig() { return getSecond(); }
    String getCommentSchema() { return getThird(); }
  }

  public static void main(String[] args) throws Exception {
    Config config = parseEnv();
    try (DatabaseInstance commentDb = new DatabaseInstance(config.getDbConfig())) {
      CommentUpdater updater = new CommentUpdater(config.getSolrUrl(), commentDb, config.getCommentSchema());
      updater.syncAll();
    }
  }

  private static Config parseEnv() {
    final var env = System.getenv();

    if (!(env.containsKey(ENV_DB_CONNECT) && env.containsKey(ENV_DB_PASS)
        && env.containsKey(ENV_DB_SCHEMA) && env.containsKey(ENV_DB_USER)
        && env.containsKey(ENV_SOLR_URL))
    ) {
      System.err.println(ERR_BAD_ENV);
      System.exit(1);
    }

    final var schema = env.get(ENV_DB_SCHEMA);
    return new Config(
      env.get(ENV_SOLR_URL),
      SimpleDbConfig.create(
        SupportedPlatform.ORACLE,
        env.get(ENV_DB_CONNECT),
        env.get(ENV_DB_USER),
        env.get(ENV_DB_PASS)
      ),
      schema.endsWith(".") ? schema : schema + "."
    );
  }

}
