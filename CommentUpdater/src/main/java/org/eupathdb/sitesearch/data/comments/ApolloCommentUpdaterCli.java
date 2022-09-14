package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public class ApolloCommentUpdaterCli extends CommentUpdaterCli {
  
  private final static String DB_CONNECT = "APPDB_CONNECT";
  private final static String DB_LOGIN = "APPDB_LOGIN";
  private final static String DB_PASSWORD = "APPDB_PASSWORD";
  private final static String SOLR_URL = "SOLR_URL";
  private final static String PROJECT_ID = "PROJECT_ID";
 
  public static void main(String[] args) throws Exception {

    String[] envVarKeys = {DB_CONNECT, DB_LOGIN, DB_PASSWORD, SOLR_URL, PROJECT_ID};
    validateEnv(envVarKeys);

    final var env = System.getenv();
    SimpleDbConfig dbConf = SimpleDbConfig.create(
        SupportedPlatform.ORACLE,
        env.get(DB_CONNECT),
        env.get(DB_LOGIN),
        env.get(DB_PASSWORD)
      );
    
    try (DatabaseInstance appDb = new DatabaseInstance(dbConf)) {
      ApolloCommentUpdater commentUpdater = new ApolloCommentUpdater(env.get(SOLR_URL), appDb, env.get(PROJECT_ID));
      commentUpdater.syncAll();
    }
  }  
}
