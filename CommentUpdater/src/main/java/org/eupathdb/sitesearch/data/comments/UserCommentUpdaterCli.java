package org.eupathdb.sitesearch.data.comments;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public class UserCommentUpdaterCli extends CommentUpdaterCli {
  
  private final static String DB_CONNECT = "USERDB_CONNECT";
  private final static String DB_LOGIN = "USERDB_LOGIN";
  private final static String DB_PASSWORD = "USERDB_PASSWORD";
  private final static String SOLR_URL = "SOLR_URL";
  private final static String USERDB_SCHEMA = "USERDB_SCHEMA";
 
  public static void main(String[] args) throws Exception {
    String[] envVarKeys = {DB_CONNECT, DB_LOGIN, DB_PASSWORD, SOLR_URL, USERDB_SCHEMA};
    validateEnv(envVarKeys);

    final var env = System.getenv();    
    SimpleDbConfig dbConf = SimpleDbConfig.create(
        SupportedPlatform.ORACLE,
        env.get(DB_CONNECT),
        env.get(DB_LOGIN),
        env.get(DB_PASSWORD)
      );
    
    try (DatabaseInstance userDb = new DatabaseInstance(dbConf)) {
      UserCommentUpdater commentUpdater = new UserCommentUpdater(env.get(SOLR_URL), userDb, env.get(USERDB_SCHEMA));
      commentUpdater.syncAll();
    }
  }  
}