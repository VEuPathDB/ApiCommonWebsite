/**

Runs sql queries against the DBPlatform returned by
org.gusdb.wdk.model.WdkModel.getAuthenticationPlatform().
This should correspond to the value of authenticationConnectionUrl 
in model-config.xml

'dbInfo' map holds database meta data.

Example usage:
<%@ taglib prefix="api" uri="http://apidb.org/taglib" %>
<api:wdkCacheDB var="cacheTable"/>
${cacheTable.dbInfo['system_date']} 

**/
package org.apidb.apicommon.taglib.wdk.info;

import org.apidb.apicommon.taglib.wdk.WdkTagBase;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;

import java.util.Map;
import java.util.HashMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.servlet.jsp.JspException;


public class CacheDbTag extends WdkTagBase {

    private String var;
    
    private DBPlatform loginPlatform;
    private String answerSchema;
    public HashMap dbInfo;
    public HashMap cacheState;
    
    public void doTag() throws JspException {
        super.doTag();
        
        // derived from org.gusdb.wdk.model.user.AnswerFactory
        loginPlatform = wdkModel.getAuthenticationPlatform();
        String answerSchema = wdkModel.getModelConfig().getUserDB().getWdkEngineSchema();
        
        setDbState();
        this.getRequest().setAttribute(var, this);

    }
    public Map getdbInfo() {
        return dbInfo;
    }
    public void setDbState() throws JspException {
        dbInfo = new HashMap<String, String>();
        StringBuffer sql = new StringBuffer();
        
        // column names will be lower-cased keys in dbInfo map
        sql.append(" select                                                          ");
        sql.append(" global_name,                                                    ");
        sql.append(" ver.banner version,                                             ");
        sql.append(" UTL_INADDR.get_host_name as server_name,                        ");
        sql.append(" UTL_INADDR.get_host_address as server_ip,                       ");
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
        
        try {
            makeAnswerMap(sql, dbInfo);
        } catch (SQLException sqle) {
            throw new JspException(sqle);
        }
    }

    // incomplete, non-functional
    public void setCacheState() throws JspException {
        cacheState = new HashMap<String, String>();
        StringBuffer sql = new StringBuffer();
        
        // column names will be lower-cased keys in dbInfo map
        sql.append(" select                                                          ");
        
        try {
            makeAnswerMap(sql, cacheState);
        } catch (SQLException sqle) {
            throw new JspException(sqle);
        }
    }
    
    private void makeAnswerMap(StringBuffer sql, Map map) throws SQLException {
        ResultSet rs = null;
        PreparedStatement ps = null;
        
        try {
            DataSource dataSource = loginPlatform.getDataSource();
            ps = SqlUtils.getPreparedStatement(dataSource, sql.toString());
            rs = ps.executeQuery();
           if (rs.next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int numColumns = rsmd.getColumnCount();
                for (int i=1; i<numColumns+1; i++) {
                    String columnName = rsmd.getColumnName(i).toLowerCase();
                    map.put(columnName, rs.getString(columnName) );
                }
            }
        } finally {
            SqlUtils.closeResultSet(rs);
        }
    }


    public void setVar(String var) {
        this.var = var;
    }

}
