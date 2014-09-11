package org.apidb.apicommon.model.migrate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;

public class B19_To_B20_Migration {

    private static final Logger LOG = Logger.getLogger(B19_To_B20_Migration.class);
    
    private static boolean TEST = false;
    
    // SQL we use to execute the updates
    private static String SELECT_GB_USERNAMES_SQL = "select userid, username, sessionid from gbrowseusers.session_tbl";
    private static String GET_WDK_UID_BY_EMAIL_SQL = "select user_id from userlogins4.users where email = ?";
    private static String UPDATE_GB_USERNAME_SQL = "update gbrowseusers.session_tbl set username = ? where username = ?";
    private static String DELETE_OUTDATED_MAPPINGS_SQL = "delete from gbrowseusers.session_tbl where userid = ?";
    private static String DELETE_OUTDATED_USERS_SQL = "delete from gbrowseusers.users where userid = ?"; //userid
    private static String DELETE_OUTDATED_SESSIONS_SQL = "delete from gbrowseusers.sessions where id = ?"; // sessionid
    
    private static class UserMapping {
    	int userid; String username, sessionid;
    	public UserMapping(int userid, String username, String sessionid) {
    		this.userid = userid; this.username = username; this.sessionid = sessionid;
    	}
    	@Override public String toString() {
    		return "[ " + userid + ", " + username + ", " + sessionid + " ]";
    	}
    }
    
    public static void main(final String[] args) {
    	DatabaseInstance userDb = parseArgs(args);
        try {
            userDb.initialize("UserDb");
            migrateGBrowseIds(userDb.getDataSource());
        } finally {
            try { userDb.close(); } catch (Exception e) { LOG.error(e); }
        }
    }

    private static DatabaseInstance parseArgs(final String[] args) {
        if (args.length == 3 || args.length == 4 && args[0].equals("-test")) {
        	final int argOffset = (args.length == 4 ? 1 : 0);
        	TEST = (args.length == 4);
            return new DatabaseInstance(new SimpleDbConfig() {
                @Override public String getConnectionUrl() { return args[0+argOffset]; }
                @Override public String getLogin() { return args[1+argOffset]; }
                @Override public String getPassword() { return args[2+argOffset]; }
                @Override public SupportedPlatform getPlatformEnum() { return SupportedPlatform.ORACLE; }
                @Override public short getConnectionPoolSize() { return 3; }
            });
        }
        System.err.println("USAGE: Exactly 3 or 4 arguments: [-test] <userDbJdbcUrl> <username> <password>");
        System.exit(1);
        return null; // will not reach here
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
            if (FormatUtil.isInteger(email)) continue; // already been converted
            querier.executeQuery(new String[]{ email }, new ResultSetHandler(){
                @Override public void handleResult(ResultSet rs) throws SQLException {
                    if (rs.next()) {
                        updateParams.add(new Object[]{ username.replace(email, String.valueOf(rs.getInt(1))), username });
                    } else {
                    	// this user is out of date; remove user, session, and any uploads
                        LOG.warn("Unable to convert GBrowse user: " + user);
                        userIdsToRemove.add(new Object[]{ user.userid });
                        sessionIdsToRemove.add(new Object[]{ user.sessionid });
                    }
                }
            });
        }
        
        // update each ID to new value containing uid instead of email
        if (TEST) {
        	System.out.println("Gbrowse usernames to update:\n");
            System.out.println(updateParams);
            System.out.println("UserIds to remove:\n");
            System.out.println(userIdsToRemove);
            System.out.println("SessionIds to remove:\n");
            System.out.println(sessionIdsToRemove);
        } else {
        	System.out.println("Updating Gbrowse usernames...");
            new SQLRunner(ds, UPDATE_GB_USERNAME_SQL)
                .executeUpdateBatch(updateParams);
            System.out.println("Deleting out-of-date user mappings...");
            new SQLRunner(ds, DELETE_OUTDATED_MAPPINGS_SQL)
            	.executeUpdateBatch(userIdsToRemove);
            System.out.println("Deleting out-of-date users...");
            new SQLRunner(ds, DELETE_OUTDATED_USERS_SQL)
        	    .executeUpdateBatch(userIdsToRemove);
            System.out.println("Deleting out-of-date sessions...");
            new SQLRunner(ds, DELETE_OUTDATED_SESSIONS_SQL)
        	    .executeUpdateBatch(sessionIdsToRemove);
        }
    }
}
