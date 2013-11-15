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
	private static String GB_SESSION_TABLE = "gbrowseusers.session_tbl";
	private static String WDK_USER_TABLE = "userlogins4.users";

	private static String SELECT_GB_USERNAMES_SQL = "select username from " + GB_SESSION_TABLE;
	private static String GET_WDK_UID_BY_EMAIL_SQL = "select user_id from " + WDK_USER_TABLE + " where email = ?";
	private static String UPDATE_GB_USERNAME_SQL = "update " + GB_SESSION_TABLE + " set username = ? where username = ?";
	
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
		
		final List<String> oldIds = new ArrayList<>();
		final BasicArgumentBatch updateParams = new BasicArgumentBatch();
		
		// get current list of usernames from GBrowse sessions table
		new SQLRunner(ds, SELECT_GB_USERNAMES_SQL)
			.executeQuery(new ResultSetHandler() {
				@Override public void handleResult(ResultSet rs) throws SQLException {
					while (rs.next()) oldIds.add(rs.getString(1));}});
		
		// create new gbrowse id by fetching uid from email and add to update params; no, this is not very efficient
		SQLRunner querier = new SQLRunner(ds, GET_WDK_UID_BY_EMAIL_SQL);
		for (final String oldId : oldIds) {
			final String email = oldId.substring(0, oldId.lastIndexOf("-"));
			querier.executeQuery(new String[]{ email }, new ResultSetHandler(){
				@Override public void handleResult(ResultSet rs) throws SQLException {
					if (rs.next()) {
						updateParams.add(new Object[]{ oldId, oldId.replace(email, String.valueOf(rs.getInt(1))) });
					} else {
						LOG.warn("Unable to convert GBrowse user ID: " + oldId);
					}
				}
			});
		}
		
		// update each ID to new value containing uid instead of email
		if (TEST) {
			System.out.println(updateParams);
		} else {
			new SQLRunner(ds, UPDATE_GB_USERNAME_SQL)
				.executeBatchUpdate(updateParams);
		}
	}
}
