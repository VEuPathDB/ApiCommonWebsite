package org.apidb.apicommon.model.migrate;

import javax.sql.DataSource;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.platform.Oracle;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.slowquery.QueryLogger;

public class B32_To_B33_Migration {

  private static final boolean WRITE_TO_DB = true;     // keep off to check generated SQL
  private static final boolean REPLICATED_DBS = false; // keep off until testing on apicommDev

  private static final String PRIMARY_DB_CONNECTION_URL = "jdbc:oracle:oci:@rm9972"; // to be apicommDevN
  private static final String REPLICATED_DB_CONNECTION_URL = "jdbc:oracle:oci:@apicommDevS";

  private static final String DB_USER = "wdkmaint";

  private static final String ACCOUNT_DB_SCHEMA = "wdkmaint.";
  private static final String USER_DB_SCHEMA = "userlogins5.";

  private static final String TABLE_USERS = "users";
  private static final String TABLE_ACCOUNTS = "accounts";

  private static final String SEQUENCE_START_NUM_MACRO = "$$sequence_start_macro$$";

  private static final String CREATE_ACCOUNT_USER_ID_SEQUENCE =
      "CREATE SEQUENCE " + ACCOUNT_DB_SCHEMA + TABLE_ACCOUNTS + "_PKSEQ" +
      " MINVALUE 1 MAXVALUE 9999999999999999999999999999" +
      " INCREMENT BY 10" +
      " START WITH " + SEQUENCE_START_NUM_MACRO +
      " CACHE 20 NOORDER NOCYCLE";

  private static final String CREATE_ACCOUNT_TABLE_SQL =
      "create table " + ACCOUNT_DB_SCHEMA + TABLE_ACCOUNTS + " as ( " +
      "  select user_id, email, passwd, is_guest, signature, address as stable_id, register_time, last_active as last_login " +
      "  from " + USER_DB_SCHEMA + TABLE_USERS +
      ")";

  private static final String SELECT_USER_PROPS_SQL_SUFFIX =
      " from " + USER_DB_SCHEMA + TABLE_USERS + " where is_guest = 0 ";

  private static final String CREATE_ACCOUNT_PROPS_TABLE_SQL =
      "create table " + ACCOUNT_DB_SCHEMA + "account_properties as ( " +
      "  select user_id, 'first_name' as key, first_name as value " + SELECT_USER_PROPS_SQL_SUFFIX +
      "  union " +
      "  select user_id, 'middle_name' as key, middle_name as value " + SELECT_USER_PROPS_SQL_SUFFIX +
      "  union " +
      "  select user_id, 'last_name' as key, last_name as value " + SELECT_USER_PROPS_SQL_SUFFIX +
      "  union " +
      "  select user_id, 'organization' as key, organization as value " + SELECT_USER_PROPS_SQL_SUFFIX +
      ")";

  private static final String BACK_UP_USERS_TABLE =
      "create table " + USER_DB_SCHEMA + "users_backup as (" +
      "  select * from " + USER_DB_SCHEMA + TABLE_USERS +
      ")";

  private static final String DROP_COLS_FROM_USERS_TABLE =
      "alter table " + USER_DB_SCHEMA + TABLE_USERS + " drop column" +
      " EMAIL, PASSWD, SIGNATURE, REGISTER_TIME, LAST_ACTIVE, LAST_NAME," +
      " FIRST_NAME, MIDDLE_NAME, TITLE, ORGANIZATION, DEPARTMENT, ADDRESS," +
      " CITY, STATE, ZIP_CODE, PHONE_NUMBER, COUNTRY, PREV_USER_ID, MIGRATION_ID";

  // Don't have to do this with the latest plan
  //private static final String RENAME_USERS_TABLE =
  //    "ALTER TABLE " + USER_DB_SCHEMA + UserFactory.TABLE_USERS +
  //    "  RENAME TO " + USER_DB_SCHEMA + UserFactory.TABLE_USERS;
      
  private static final SqlGetter[] PRIMARY_SQLS_TO_RUN = {
      // create a new sequence in account DB with the start ID of the old sequence
      createAccountSequenceFromUserSequence(),
      // create new account tables from user table
      doSql(CREATE_ACCOUNT_TABLE_SQL),
      doSql(CREATE_ACCOUNT_PROPS_TABLE_SQL),
      // make a copy of the users table
      doSql(BACK_UP_USERS_TABLE),
      // trim columns off existing user table
      doSql(DROP_COLS_FROM_USERS_TABLE)
  };

  private static final SqlGetter[] REPLICATED_SQLS_TO_RUN = {
      createAccountSequenceFromUserSequence()
  };

  public static void main(String[] args) {
    if (args.length != 1 || args[0].trim().isEmpty()) {
      System.err.println("USAGE: fgpJava " + B32_To_B33_Migration.class.getName() + " <" + DB_USER + "_password>");
      System.exit(1);
    }
    String dbPassword = args[0];
    QueryLogger.setInactive();
    runSqls(PRIMARY_DB_CONNECTION_URL, PRIMARY_SQLS_TO_RUN, dbPassword);
    if (REPLICATED_DBS) {
      runSqls(REPLICATED_DB_CONNECTION_URL, REPLICATED_SQLS_TO_RUN, dbPassword);
    }
  }

  private static void runSqls(String connectionUrl, SqlGetter[] sqlsToRun, String dbPassword) {
    SimpleDbConfig dbConfig = SimpleDbConfig.create(SupportedPlatform.ORACLE, connectionUrl, DB_USER, dbPassword);
    try (DatabaseInstance db = new DatabaseInstance(dbConfig)) {
      DataSource ds = db.getDataSource();
      for (SqlGetter sqlGen : sqlsToRun) {
        String sql = sqlGen.getSql(ds);
        System.out.println("Executing on " + connectionUrl + ":" + FormatUtil.NL + sql);
        if (WRITE_TO_DB) {
          new SQLRunner(ds, sql).executeStatement();
        }
      }
    }
    catch (Exception e) {
      System.err.println("Error while executing migration: " + FormatUtil.getStackTrace(e));
      System.exit(2);
    }
  }

  private static interface SqlGetter {
    public String getSql(DataSource ds) throws Exception;
  }

  private static SqlGetter doSql(final String sql) {
    return new SqlGetter() {
      @Override public String getSql(DataSource ds) {
        return sql;
      }
    };
  }

  private static SqlGetter createAccountSequenceFromUserSequence() {
    return new SqlGetter() {
      @Override public String getSql(DataSource ds) throws Exception {
        Integer nextId = new Oracle().getNextId(ds, USER_DB_SCHEMA, TABLE_USERS);
        return CREATE_ACCOUNT_USER_ID_SEQUENCE.replace(SEQUENCE_START_NUM_MACRO, nextId.toString());
      }
    };
  }
}
