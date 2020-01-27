package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public class CommentUpdaterCli {

  private static class Config extends ThreeTuple<String,ConnectionPoolConfig,String> {
    public Config(String solrUrl, ConnectionPoolConfig commentDbConfig, String commentDbSchema) {
      super(solrUrl, commentDbConfig, commentDbSchema);
    }
    public String getSolrUrl() { return getFirst(); }
    public ConnectionPoolConfig getDbConfig() { return getSecond(); }
    public String getCommentSchema() { return getThird(); }
  }

  public static void main(String[] args) throws Exception {
    Config config = parseArgs(args);
    try (DatabaseInstance commentDb = new DatabaseInstance(config.getDbConfig())) {
      CommentUpdater updater = new CommentUpdater(config.getSolrUrl(), commentDb, config.getCommentSchema());
      updater.performSync();
    }
  }

  private static Config parseArgs(String[] args) {
    if (args.length != 5) {
      System.err.println("\nUSAGE: fgpJava " + CommentUpdaterCli.class.getName() +
          " <solrUrl> <commentDbConnectionString> <commentDbUser> <commentDbPassword> <commentDbSchema>\n");
      System.exit(1);
    }
    String schema = args[4].endsWith(".") ? args[4] : args[4] + ".";
    return new Config(args[0], SimpleDbConfig.create(SupportedPlatform.ORACLE, args[1], args[2], args[3]), schema);
  }

}
