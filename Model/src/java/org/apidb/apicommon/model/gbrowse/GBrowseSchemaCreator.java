package org.apidb.apicommon.model.gbrowse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.db.SqlScriptRunner;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.runtime.GusHome;

public class GBrowseSchemaCreator {

  private static enum Op { CREATE, DROP }

  private static final String SCRIPT_HOME = "/data/ApiCommonWebsite/Model/";
  
  private static final String ORACLE_SCRIPT = "createGbrowseSchema-Oracle.sql";
  private static final String POSTGRES_SCRIPT = "createGbrowseSchema-Postgres.sql";
  private static final String DROP_SCRIPT = "dropGbrowseSchema.sql";

  private static final PrintStream out = System.out;
  private static final PrintStream err = System.err;
  private static final String NL = System.getProperty("line.separator");

  private static final String USAGE = new StringBuilder()
    .append("USAGE: java ").append(GBrowseSchemaCreator.class.getName())
    .append(" <connectionString> <username> <password> <schemaName> <dbType> [-drop]")
    .append(NL).toString();

  private static final CharSequence USER_SCHEMA_KEY = "${USER_SCHEMA_KEY}";
  
  public static void main(String[] args) {
    GBrowseSchemaCreator worker = null;
    try {
      Op operation = determineOp(args);
      GBrowseConnectionConfig config = new GBrowseConnectionConfig(args);
      
      out.print("Will perform operation '" + operation + "' using the " +
      		"following connection information: " + NL + config);

      worker = new GBrowseSchemaCreator(config);
      worker.runOp(operation);
    }
    catch (Exception e) {
      e.printStackTrace(err);
    }
    finally {
      worker.shutDownQuietly();
    }
  }

  private DatabaseInstance _db;
  private String _schemaPrefix;
  
  public GBrowseSchemaCreator(GBrowseConnectionConfig config) {
    _schemaPrefix = config.getSchema();
    _db = new DatabaseInstance(config, "gbrowseUsers");
  }

  public void shutDownQuietly() {
    try {
      _db.close();
    }
    catch (Exception e) {
      e.printStackTrace(err);
    }
  }
  
  public void runOp(Op operation) throws IOException, SQLException {
    switch (operation) {
      case CREATE:
        switch (_db.getConfig().getPlatformEnum()) {
          case ORACLE:
            runScript(ORACLE_SCRIPT);
            break;
          case POSTGRESQL:
            runScript(POSTGRES_SCRIPT);
            break;
        }
        break;
      case DROP:
        runScript(DROP_SCRIPT);
        break;
    }
  }
  
  private void runScript(String scriptName) throws IOException, SQLException {
    File scriptPath = new File(GusHome.getGusHome() + SCRIPT_HOME + scriptName);
    String scriptContents = getAlteredScript(scriptPath, _schemaPrefix);
    Connection conn = null;
    try {
      conn = _db.getDataSource().getConnection();
      out.println("Opened connection. Running SQL in: " + scriptPath.getAbsolutePath());
      SqlScriptRunner runner = new SqlScriptRunner(conn, true, true);
      PrintWriter errWriter = new PrintWriter(err);
      runner.setLogWriter(errWriter);
      runner.setErrorLogWriter(errWriter);
      runner.runScript(new InputStreamReader(IoUtil.getStreamFromString(scriptContents)));
      out.println("Ran script successfully.");
    }
    finally {
      SqlUtils.closeQuietly(conn);
    }
  }

  private String getAlteredScript(File scriptPath, String schemaName) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(scriptPath));
      StringBuilder alteredFile = new StringBuilder();
      while (br.ready()) {
        alteredFile.append(br.readLine().replace(USER_SCHEMA_KEY, schemaName)).append(NL);
      }
      return alteredFile.toString();
    }
    finally {
      IoUtil.closeQuietly(br);
    }
  }

  private static Op determineOp(String[] args) {
    switch (args.length) {
      case 5:
        return Op.CREATE;
      case 6:
        if (args[5].equals("-drop")) {
          return Op.DROP;
        }
        // fall through if not -drop
      default:
        throw new IllegalArgumentException(USAGE);
    }
  }
}
