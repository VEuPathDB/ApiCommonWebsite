package org.apidb.apicommon.jmx.mbeans.wdk;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;

public abstract class AbstractDB extends BeanBase {


  HashMap<String, String> metaDataMap;
  HashMap<String, String> servernameDataMap;
  ArrayList<Map<String, String>> dblinkList;
  DataSource dataSource;
  
  private static final Logger logger = Logger.getLogger(AbstractDB.class);

  /**
    ServletContext sc
    String type - QueryPlatform or UserPlatform, to match the getter method
                  in WdkModel.
  **/
  public AbstractDB(String type) {
    super();

    DBPlatform platform = getPlatform(type);
    dataSource = platform.getDataSource();
    init();
  }

  private void init() {
    populateDatabaseMetaDataMap();
    populateServernameDataMap();
    populateDblinkList();
  }

  public void refresh() { init(); }

  public ArrayList<Map<String,String>> getDblinkList() { return dblinkList; }

  public String getserver_name() { return servernameDataMap.get("server_name"); }
  public String getserver_ip() { return servernameDataMap.get("server_ip"); }

  public String getglobal_name() { return metaDataMap.get("global_name"); }
  public String getversion() { return metaDataMap.get("version"); }
  public String getsystem_date() { return metaDataMap.get("system_date"); }
  public String getlogin() { return metaDataMap.get("login"); }
  public String getservice_name() { return metaDataMap.get("service_name"); }
  public String getdb_name() { return metaDataMap.get("db_name"); }
  public String getdb_unique_name() { return metaDataMap.get("db_unique_name"); }
  public String getinstance_name() { return metaDataMap.get("instance_name"); }
  public String getdb_domain() { return metaDataMap.get("db_domain"); }
  public String getclient_host() { return metaDataMap.get("client_host"); }
  public String getos_user() { return metaDataMap.get("os_user"); }
  public String getcurrent_userid() { return metaDataMap.get("current_userid"); }
  public String getsession_user() { return metaDataMap.get("session_user"); }
  public String getsession_userid() { return metaDataMap.get("session_userid"); }


  private DBPlatform getPlatform(String type) {
    java.lang.reflect.Method method = null;
    DBPlatform platform = null;
    String methodName = "get" + type;
    try {
      method = wdkModel.getClass().getMethod(methodName);
    } catch (SecurityException se) {
      logger.fatal(se);
    } catch (NoSuchMethodException nsme) {
      logger.fatal(nsme);
    }

    try {
      platform = (DBPlatform)method.invoke(wdkModel);
    } catch (IllegalArgumentException iae) {
      logger.fatal(iae);
    } catch (IllegalAccessException iae) {
      logger.fatal(iae);
    } catch (InvocationTargetException ite) {
      logger.fatal(ite);
    }
    
    return platform;
  }

  // refactor this for Oracle vs Postgres
  private String getServerNameSql() {
    StringBuffer sql = new StringBuffer();
    sql.append(" select                                     ");
    sql.append(" UTL_INADDR.get_host_name as server_name,   ");
    sql.append(" UTL_INADDR.get_host_address as server_ip   ");
    sql.append(" from dual                                  ");
    return sql.toString();
  }

  // refactor this for Oracle vs Postgres
  private String getMetaDataSql() {
    StringBuffer sql = new StringBuffer();
    
    // column names will be lower-cased keys in metaDataMap
    sql.append(" select                                                          ");
    sql.append(" global_name,                                                    ");
    sql.append(" ver.banner version,                                             ");
    sql.append(" to_char(sysdate, 'Dy DD-Mon-YYYY HH24:MI:SS') system_date,      ");
    sql.append(" sys_context('USERENV', 'SESSION_USER'       ) login,            ");
    sql.append(" sys_context('userenv', 'SERVICE_NAME'       ) service_name,     ");
    sql.append(" sys_context('userenv', 'DB_NAME'            ) db_name,          ");
    sql.append(" sys_context('USERENV', 'DB_UNIQUE_NAME'     ) db_unique_name,   ");
    sql.append(" sys_context('USERENV', 'INSTANCE_NAME'      ) instance_name,    ");
    sql.append(" sys_context('USERENV', 'DB_DOMAIN'          ) db_domain,        ");
    sql.append(" sys_context('USERENV', 'HOST'               ) client_host,      ");
    sql.append(" sys_context('USERENV', 'OS_USER'            ) os_user,          ");
    sql.append(" sys_context('USERENV', 'CURRENT_USERID'     ) current_userid,   ");
    sql.append(" sys_context('USERENV', 'SESSION_USER'       ) session_user,     ");
    sql.append(" sys_context('USERENV', 'SESSION_USERID'     ) session_userid    ");
    sql.append(" from global_name, v$version ver                                 ");
    sql.append(" where lower(ver.banner) like '%oracle%'                         ");
    
    return sql.toString();
  }

  private String getDblinkSql() {
    StringBuffer sql = new StringBuffer();

    sql.append(" select             ");
    sql.append(" owner owner,       ");
    sql.append(" db_link db_link,   ");
    sql.append(" username username, ");
    sql.append(" host host,         ");
    sql.append(" created created    ");
    sql.append(" from all_db_links  ");
    
    return sql.toString();
  }

  private void populateDblinkList() {
    String sql = getDblinkSql();
    dblinkList = new ArrayList<Map<String, String>>();
    ResultSet rs = null;
    PreparedStatement ps = null;

    try {
      ps = SqlUtils.getPreparedStatement(dataSource, sql);
      rs = ps.executeQuery();
     while (rs.next()) {
        HashMap<String, String> map = new HashMap<String, String>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i=1; i<numColumns+1; i++) {
          String columnName = rsmd.getColumnName(i).toLowerCase();
          map.put(columnName, rs.getString(columnName) );
        }
        dblinkList.add(map);
      }
    } catch (SQLException sqle) {
      logger.fatal(sqle);
    } finally {
        SqlUtils.closeResultSet(rs);
    }

  }
  
  private void populateServernameDataMap() {
    String sql = getServerNameSql();
    servernameDataMap = new HashMap<String, String>();
    ResultSet rs = null;
    PreparedStatement ps = null;
    logger.debug("querying database for servername information");    
    try {
      ps = SqlUtils.getPreparedStatement(dataSource, sql);
      rs = ps.executeQuery();
     if (rs.next()) {
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i=1; i<numColumns+1; i++) {
          String columnName = rsmd.getColumnName(i).toLowerCase();
          servernameDataMap.put(columnName, rs.getString(columnName) );
        }
      }
    } catch (SQLException sqle) {
      logger.fatal(sqle);
    } finally {
        SqlUtils.closeResultSet(rs);
    }  
  }
  
  private void populateDatabaseMetaDataMap() {
    String sql = getMetaDataSql();
    metaDataMap = new HashMap<String, String>();
    ResultSet rs = null;
    PreparedStatement ps = null;
    logger.debug("querying database for misc. information");    
    try {
      ps = SqlUtils.getPreparedStatement(dataSource, sql);
      rs = ps.executeQuery();
     if (rs.next()) {
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i=1; i<numColumns+1; i++) {
          String columnName = rsmd.getColumnName(i).toLowerCase();
          metaDataMap.put(columnName, rs.getString(columnName) );
        }
      }
    } catch (SQLException sqle) {
      logger.fatal(sqle);
    } finally {
        SqlUtils.closeResultSet(rs);
    }
  }

}
