package org.apidb.apicommon.model.gbrowse;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;

public class GBrowseConnectionConfig implements ConnectionPoolConfig {

  private static final String NL = System.getProperty("line.separator");
  
  // vars needed by connection pool
  private String _connectionString;
  private String _username;
  private String _password;
  private SupportedPlatform _dbType;
  
  // var needed to filter SQL scripts
  private String _schemaName;

  public GBrowseConnectionConfig(String[] args) {
    if (args.length != 5 && args.length != 6) {
      throw new IllegalArgumentException("Must have 5 or more arguments in passed String[].");
    }
    _connectionString = args[0];
    _username = args[1];
    _password = args[2];
    _schemaName = args[3];
    _dbType = SupportedPlatform.toPlatform(args[4]);
  }
  
  public String getSchema() { return _schemaName; }
  
  @Override public String getLogin() { return _username; }
  @Override public String getPassword() { return _password; }
  @Override public String getConnectionUrl() { return _connectionString; }
  @Override public SupportedPlatform getPlatformEnum() { return _dbType; }
  @Override public short getMaxActive() { return 1; }
  @Override public short getMaxIdle() { return 5; }
  @Override public short getMinIdle() { return 0; }
  @Override public long getMaxWait() { return 100; }
  @Override public boolean isShowConnections() { return false; }
  @Override public long getShowConnectionsInterval() { return 0; }
  @Override public long getShowConnectionsDuration() { return 0; }
  @Override public boolean getDefaultAutoCommit() { return true; }
  @Override public boolean getDefaultReadOnly() { return false; }
  @Override public String getDriverInitClass() { return null; }

  @Override
  public String toString() {
    return new StringBuilder("Config {").append(NL)
        .append("  connectionString: ").append(_connectionString).append(NL)
        .append("  username:         ").append(_username).append(NL)
        .append("  password:         ").append(_password).append(NL)
        .append("  dbType:           ").append(_dbType.toString()).append(NL)
        .append("  schemaName:       ").append(_schemaName).append(NL)
        .append("}").append(NL).toString();
  }
}
