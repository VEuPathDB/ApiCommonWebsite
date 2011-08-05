package org.apidb.apicommonwebsite.model.fusiontables;

// Get data from a Google Fusion Table.
// Requires the Google GData Client library,
// which in turn requires the
// Google Collection library. These can be downloaded from
// http://code.google.com/p/gdata-java-client/downloads/list and
// http://code.google.com/p/google-collections/downloads/list.

import com.google.gdata.client.ClientLoginAccountType;
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;

import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;

import java.lang.Math;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.jdbc.driver.OracleDriver;

/**
 * This is a modified copy of ApiExample.java --
 * Java example using the Google Fusion Tables API
 * to query, insert, update, and delete.
 * Uses the Google GDATA core library.
 *
 * @author googletables-feedback@google.com (Google Fusion Tables Team)
 */
public class FusionTable {

    /**
     * Google Fusion Tables API URL stem.
     * All requests to the Google Fusion Tables server
     * begin with this URL.
     *
     * The next line is Google Fusion Tables API-specific code:
     */
    private static final String SERVICE_URL =
	"https://www.google.com/fusiontables/api/query";

    /**
     * CSV values are terminated by comma or end-of-line and consist either of
     * plain text without commas or quotes, or a quoted expression, where inner
     * quotes are escaped by doubling.
     */
    private static final Pattern CSV_VALUE_PATTERN =
	Pattern.compile("([^,\\r\\n\"]*|\"(([^\"]*\"\")*[^\"]*)\")(,|\\r?\\n)");

    /**
     * Handle to the authenticated Google Fusion Tables service.
     *
     * This code uses the GoogleService class from the
     * Google GData APIs Client Library.
     */
    private GoogleService service;

    /**
     * Two versions of FusionTable() are provided:
     * one that accepts a Google user account ID and password for authentication,
     * and one that accepts an existing auth token.
     */

    /**
     * Authenticates the given account for {@code fusiontables} service using a
     * given email ID and password.
     *
     * @param email    Google account email. (For more information, see
     *                 http://www.google.com/support/accounts.)
     * @param password Password for the given Google account.
     *
     * This code instantiates the GoogleService class from the
     * Google GData APIs Client Library,
     * passing in Google Fusion Tables API-specific parameters.
     * It then goes back to the Google GData APIs Client Library for the
     * setUserCredentials() method.
     */
    public FusionTable(String email, String password)
	throws AuthenticationException {
	service = new GoogleService("fusiontables", "fusiontables.FusionTable");
	service.setUserCredentials(email, password, ClientLoginAccountType.GOOGLE);
    }

    /**
     * Authenticates for {@code fusiontables} service using the auth token. The
     * auth token can be retrieved for an authenticated user by invoking
     * service.getAuthToken() on the email and password. The auth token can be
     * reused rather than specifying the user name and password repeatedly.
     *
     * @param authToken The auth token. (For more information, see
     *                  http://code.google.com/apis/gdata/auth.html#ClientLogin.)
     *
     * @throws AuthenticationException
     *
     * This code instantiates the GoogleService class from the
     * Google Data APIs Client Library,
     * passing in Google Fusion Tables API-specific parameters.
     * It then goes back to the Google Data APIs Client Library for the
     * setUserToken() method.
     */
    public FusionTable(String authToken) throws AuthenticationException {
	service = new GoogleService("fusiontables", "fusiontables.FusionTable");
	service.setUserToken(authToken);
    }

    /**
     * Fetches the results for a select query. Prints them to standard
     * output, surrounding every field with (@code |}.
     *
     * This code uses the GDataRequest class and getRequestFactory() method
     * from the Google Data APIs Client Library.
     * The Google Fusion Tables API-specific part is in the construction
     * of the service URL. A Google Fusion Tables API SELECT statement
     * will be passed in to this method in the selectQuery parameter.
     */
    public void loadTuningTableFromFusionTable(String datasetId, Connection dbc, String tuningTable, String suffix, String columnList) throws IOException,
																	      ServiceException, SQLException {
	String selectQuery = new String("select " + columnList + " from " + datasetId);
	URL url = new URL(
			  SERVICE_URL + "?sql=" + URLEncoder.encode(selectQuery, "UTF-8"));
	GDataRequest request = service.getRequestFactory().getRequest(RequestType.QUERY, url, ContentType.TEXT_PLAIN);

	request.execute();

	/* Loop through result set, one column per iteration */

	Scanner scanner = new Scanner(request.getResponseStream(),"UTF-8");
	boolean processingHeader = true;
	int columnNumber = 1;
	StringBuffer insertSql = new StringBuffer("insert into " + tuningTable + suffix + " (");
	StringBuffer bindings = new StringBuffer("");
	PreparedStatement insertStatement = dbc.prepareStatement("select sysdate from dual");

	while (scanner.hasNextLine()) {

	    if (processingHeader) {
		scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
		MatchResult match = scanner.match();
		String quotedString = match.group(2);
		String decoded = quotedString == null ? match.group(1)
		    : quotedString.replaceAll("\"\"", "\"");
		insertSql.append(decoded);
		bindings.append("?");
		if (!match.group(4).equals(",")) { // last column; process row
		    insertSql.append(") values (");
		    insertSql.append(bindings);
		    insertSql.append(")");
		    // System.out.println ("finished insert statement:\n" + insertSql);
		    insertStatement = dbc.prepareStatement(insertSql.toString());
		    processingHeader = false;
		} else {
		    insertSql.append(", ");
		    bindings.append(", ");
		}
	    } else {
		scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
		MatchResult match = scanner.match();
		String quotedString = match.group(2);
		String decoded = quotedString == null ? match.group(1)
		    : quotedString.replaceAll("\"\"", "\"");
		insertStatement.setString(columnNumber, decoded);
		columnNumber++;
		if (!match.group(4).equals(",")) { // last column; process row
		    insertStatement.executeQuery();
		    columnNumber = 1;
		}
	    }
	}
    }

    /**
     * Fetches the results for a select query. Prints them to standard
     * output, surrounding every field with (@code |}.
     *
     * This code uses the GDataRequest class and getRequestFactory() method
     * from the Google Data APIs Client Library.
     * The Google Fusion Tables API-specific part is in the construction
     * of the service URL. A Google Fusion Tables API SELECT statement
     * will be passed in to this method in the selectQuery parameter.
     */
    public void createTuningTable(String datasetId, Connection dbc, String tuningTable, String suffix, String columnList) throws IOException,
																 ServiceException, SQLException {
	String selectQuery = new String("select " + columnList + " from " + datasetId);
	URL url = new URL(SERVICE_URL + "?sql=" + URLEncoder.encode(selectQuery, "UTF-8"));
	GDataRequest request = service.getRequestFactory().getRequest(RequestType.QUERY, url, ContentType.TEXT_PLAIN);
	
	request.execute();
	
	/* Prints the results of the query.                */
	/* No Google Fusion Tables API-specific code here. */

	Scanner scanner = new Scanner(request.getResponseStream(),"UTF-8");
	boolean processingHeader = true;
	int columnNumber = 0;
	int columnCount = 0;
	int MAXCOLUMNS = 30;
	int[] columnLength;
	columnLength = new int[MAXCOLUMNS];
	String[] columnName;
	columnName = new String[MAXCOLUMNS];

	while (scanner.hasNextLine()) {

	    if (processingHeader) {
		scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
		MatchResult match = scanner.match();
		String quotedString = match.group(2);
		String decoded = quotedString == null ? match.group(1)
		    : quotedString.replaceAll("\"\"", "\"");
		columnName[columnNumber++] = decoded;
		if (!match.group(4).equals(",")) { // last column; process row
		    processingHeader = false;
		    columnCount = columnNumber;
		    columnNumber = 0;
		}
	    } else {
		scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
		MatchResult match = scanner.match();
		String quotedString = match.group(2);
		String decoded = quotedString == null ? match.group(1)
		    : quotedString.replaceAll("\"\"", "\"");
		columnLength[columnNumber] = Math.max(columnLength[columnNumber], decoded.length());
		columnNumber++;
		if (!match.group(4).equals(",")) { // last column; process row
		    columnNumber = 0;
		}
	    }
	}

	StringBuffer createSql = new StringBuffer("create table " + tuningTable + suffix + " (\n");
	for (int i = 0; i < columnCount; i++) {
	    if (i > 0) {createSql.append(",\n");}
	    createSql.append("  " + columnName[i] + " varchar2(" + columnLength[i] + ")");
	}
	createSql.append("\n)");

	// System.out.println ("finished create-table statement:\n" + createSql);
	
	Statement createStmt = dbc.createStatement();
	createStmt.executeUpdate(createSql.toString());
    }

    /**
     *
     *
     */
    public static void main(String[] args) throws ServiceException, IOException, SQLException {

	if (args.length != 3 && args.length != 4) {
	    System.err.println("usage: java FusionTable <datasetId> <tuningTable> <suffix> [ <columnList> ]");
	    System.exit(1);
	}

	String instance = "";
	String schema = "";
	String password = "";

	try {
	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    instance = in.readLine();
	    schema = in.readLine();
	    password = in.readLine();
	} catch (IOException e) {
	}
	DriverManager.registerDriver (new OracleDriver());
	Connection dbc = DriverManager.getConnection("jdbc:oracle:oci:@" + instance, schema, password);

	FusionTable ft = new FusionTable("");
	String columnList = new String( (args.length == 4) ? args[3] : "*");
	ft.createTuningTable(args[0], dbc, args[1], args[2], columnList);
	ft.loadTuningTableFromFusionTable(args[0], dbc, args[1], args[2], columnList);
    }
}
