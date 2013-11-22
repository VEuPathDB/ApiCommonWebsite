package org.apidb.apicommon.model.migrate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;

public class B19_To_B20_Migration {

    private static final Logger LOG = Logger.getLogger(B19_To_B20_Migration.class);
    
    private static final boolean TEST = true;
    
    // SQL we use to execute the updates
    private static String SELECT_GB_USERNAMES_SQL = "select username, sessionid, uploadsid from gbrowseusers.session_tbl";
    private static String GET_WDK_UID_BY_EMAIL_SQL = "select user_id from userlogins4.users where email = ?";
    private static String UPDATE_GB_USERNAME_SQL = "update gbrowseusers.session_tbl set username = ? where username = ?";
    private static String DELETE_OUTDATED_MAPPINGS_SQL = "delete from gbrowseusers.session_tbl where userid = ?";
    private static String DELETE_OUTDATED_USERS_SQL = "delet from gbrowseusers.users where userid = ?"; //userid
    private static String DELETE_OUTDATED_SESSIONS_SQL = "delete from gbrowseusers.sessions where id = ?"; // sessionid
    
    private static class UserMapping {
    	int userid; String username, sessionid;
    	public UserMapping(int userid, String username, String sessionid) {
    		this.userid = userid; this.username = username; this.sessionid = sessionid;
    	}
    }
    
    public static void main(final String[] args) {
        if (args.length != 3) {
            System.err.println("USAGE: Exactly three arguments required: <userDbJdbcUrl> <username> <password>");
            System.exit(1);
        }
        DatabaseInstance userDb = new DatabaseInstance("UserDb", new SimpleDbConfig() {
            @Override public String getConnectionUrl() { return args[0]; }
            @Override public String getLogin() { return args[1]; }
            @Override public String getPassword() { return args[2]; }
            @Override public SupportedPlatform getPlatformEnum() { return SupportedPlatform.ORACLE; }
            @Override public short getConnectionPoolSize() { return 3; }
        });
        try {
            userDb.initialize();
            migrateGBrowseIds(userDb.getDataSource());
        } finally {
            try { userDb.close(); } catch (Exception e) { LOG.error(e); }
        }
    }

    private static void migrateGBrowseIds(DataSource ds) {
        
        final List<UserMapping> users = new ArrayList<>();
        final BasicArgumentBatch updateParams = new BasicArgumentBatch();
        final BasicArgumentBatch userIdsToRemove = new BasicArgumentBatch();
        final BasicArgumentBatch sessionIdsToRemove = new BasicArgumentBatch();
        
        // get current list of usernames from GBrowse sessions table
        new SQLRunner(ds, SELECT_GB_USERNAMES_SQL)
            .executeQuery(new ResultSetHandler() {
                @Override public void handleResult(ResultSet rs) throws SQLException {
                	while (rs.next()) users.add(
                		new UserMapping(rs.getInt(1), rs.getString(2), rs.getString(3)));}});
        
        // create new gbrowse id by fetching uid from email and add to update params; no, this is not very efficient
        SQLRunner querier = new SQLRunner(ds, GET_WDK_UID_BY_EMAIL_SQL);
        for (final UserMapping user : users) {
        	final String username = user.username;
            final String email = username.substring(0, username.lastIndexOf("-"));
            querier.executeQuery(new String[]{ email }, new ResultSetHandler(){
                @Override public void handleResult(ResultSet rs) throws SQLException {
                    if (rs.next()) {
                        updateParams.add(new Object[]{ username, username.replace(email, String.valueOf(rs.getInt(1))) });
                    } else {
                    	// this user is out of date; remove user, session, and any uploads
                        LOG.warn("Unable to convert GBrowse username: " + username);
                        userIdsToRemove.add(new Object[]{ user.userid });
                        sessionIdsToRemove.add(new Object[]{ user.sessionid });
                    }
                }
            });
        }
        
        // update each ID to new value containing uid instead of email
        if (TEST) {
        	System.out.println("Gbrowse usernames to update:");
            System.out.println(updateParams);
            System.out.println("UserIds to remove");
            System.out.println(userIdsToRemove);
            System.out.println("SessionIds to remove");
            System.out.println(sessionIdsToRemove);
        } else {
            new SQLRunner(ds, UPDATE_GB_USERNAME_SQL)
                .executeUpdateBatch(updateParams);
            new SQLRunner(ds, DELETE_OUTDATED_MAPPINGS_SQL)
            	.executeUpdateBatch(userIdsToRemove);
            new SQLRunner(ds, DELETE_OUTDATED_USERS_SQL)
        	    .executeUpdateBatch(userIdsToRemove);
            new SQLRunner(ds, DELETE_OUTDATED_SESSIONS_SQL)
        	    .executeUpdateBatch(sessionIdsToRemove);
        }
    }
}
