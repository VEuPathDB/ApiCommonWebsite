package org.apidb.apicommon.service.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apidb.apicommon.model.gbrowse.GBrowseTrackStatus;
import org.apidb.apicommon.model.gbrowse.GBrowseUtils;
import org.apidb.apicommon.model.gbrowse.UploadStatus;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.user.UserService;
import org.json.JSONArray;
import org.json.JSONObject;

public class BigWigTrackService extends UserService {
	
  private static final String AUTH_COOKIE_NAME = "wdk_check_auth";
  private static final String SESSION_COOKIE_NAME = "JSESSIONID";
 
  private static final String TRACK_SOURCE_PATH_MACRO = "$$TRACK_SOURCE_PATH_MACROS$$";
  private static final String TRACK_NAME_MACRO = "$$TRACK_NAME_MACROS$$";
  private static final String BASE_URI_MACRO = "$$BASE_URL_MACRO$$";
  private static final String USER_ID_MACRO = "$$USER_ID_MACRO$$";
  private static final String USER_DATASET_ID_MACRO = "$$USER_DATASET_ID_MACRO$$";
  private static final String DATAFILE_NAME_MACRO = "$$DATFILE_NAME_MACRO$$";
  private static final String GLOBAL_READ_WRITE_PERMS = "rw-rw-rw-";

  private static final String BINARY_DATAFILE_DOWNLOAD_URL = BASE_URI_MACRO +
		  "users/" + USER_ID_MACRO + 
		  "/user-datasets/" + USER_DATASET_ID_MACRO +
		  "/user-datafiles/" + DATAFILE_NAME_MACRO;
  
  private static String CONFIGURATION_TEMPLATE = 
		  "[track_" + TRACK_NAME_MACRO + ":database]\n" +
		  "db_adaptor    = Bio::DB::BigWig\n" +
		  "db_args       = -bigwig '" + TRACK_SOURCE_PATH_MACRO + "'\n" +
		  "\n" +
		  "#>>>>>>>>>> cut here <<<<<<<<\n" +
		  "[track_" + TRACK_NAME_MACRO + "_1]\n" +
		  "database = track_" + TRACK_NAME_MACRO + "\n" +
		  "feature  = summary\n" +
		  "key      = " + TRACK_NAME_MACRO + "\n" +
		  "glyph = wiggle_whiskers\n" +
		  "autoscale = chromosome\n" +
		  "height  = 50\n"+
		  "description =\n";

	
  public BigWigTrackService(@PathParam(USER_ID_PATH_PARAM) String uid) {
    super(uid);
  }

  /**
   * Service to upload a bigwig track to the GBrowse track upload file system for a user.  This service sets
   * up the subdirectories, configuration and status files as needed and passes the job of performing the
   * streaming of the binary file to the user dataset service intended for that purpose.  The actual track name
   * is modified from the datafile name provided by incorporating the user dataset id so as to avoid name collisions.
   * If the upload request corresponds to a track name that was previously uploaded successfully, nothing will be
   * done.
   * @param datasetId
   * @param authCookie
   * @param sessionCookie
   * @param datafileName
   * @return
   * @throws WdkModelException
   */
  @GET
  @Path("user-datasets/{datasetId}/upload-bigwig-track")
  public Response loadTrack(@PathParam("datasetId") String datasetId,
		  @CookieParam(AUTH_COOKIE_NAME) Cookie authCookie,
		  @CookieParam(SESSION_COOKIE_NAME) Cookie sessionCookie,
		  @QueryParam("datafileName") String datafileName) throws WdkModelException {
	  
	//TODO - will the datafileName come URLEncoded?
	  
	// The trackName is used to create the file system scaffolding for the track to be uploaded to GBrowse.  
	String trackName = GBrowseUtils.composeTrackName(datasetId, datafileName);

	long userId = getPrivateRegisteredUser().getUserId();
	java.nio.file.Path userTracksDir = GBrowseUtils.getUserTracksDirectory(getWdkModel(), userId);
	
	// Build out the user upload id directory if necessary
	if(userTracksDir != null) {
	  Map<String, GBrowseTrackStatus> tracksStatus = GBrowseUtils.getTracksStatus(userTracksDir);
	  
    	  List<String> successfullyUploadedTracks = tracksStatus.values().stream()
        .filter(ts -> UploadStatus.COMPLETED.name().equals(ts.getStatusIndicator()))
    	    .map(ts -> ts.getName()).collect(Collectors.toList());
    	  
    	  // If the track was successfully uploaded to the GBrowse uploaded track file system, do nothing
    	  if(!successfullyUploadedTracks.contains(trackName)) {
    		String trackPath = Paths.get(userTracksDir.toString(), trackName).toString();
    		java.nio.file.Path trackSourcePath = Paths.get(trackPath, "SOURCES", trackName);
    		try {
    			
    		  // This directory likely already exists for error'd/in progress tracks
    		  IoUtil.createOpenPermsDirectory(Paths.get(trackPath));
    		  
    		  // Create status file is necessary and set it to in progress.
    		  manageStatusFile(trackPath,UploadStatus.IN_PROGRESS, "");
    		  
    		  // This directory likely already exists for error'd/in progress tracks
    		  IoUtil.createOpenPermsDirectory(Paths.get(trackPath, "SOURCES"));
    		  
    		  // This is a vanilla configuration file that assumes the datafile type to
    		  // be that of a bigwig file.
          String configurationFileData = CONFIGURATION_TEMPLATE
        		.replace(TRACK_SOURCE_PATH_MACRO, trackSourcePath.toString())
        		.replace(TRACK_NAME_MACRO, trackName);
          java.nio.file.Path configurationFilePath = Paths.get(trackPath, trackName + ".conf");
          
          // Truncates and re-writes the file for error'd/in progress tracks
          Files.write(configurationFilePath, configurationFileData.getBytes());
          GBrowseUtils.setPosixPermissions(configurationFilePath, GLOBAL_READ_WRITE_PERMS);
        }
        catch(IOException ioe) {
        	  // Report any error in the status file.
        	  manageStatusFile(trackPath, UploadStatus.ERROR, "Unable to create the custom track: " + trackName);
        	  throw new WdkModelException("Unable to create the custom track: " + trackName, ioe);
        }
    		try {
    		  // Calling the user dataset binary datafile download service to drop in the bigwig file.	
    		  String downloadUrl = BINARY_DATAFILE_DOWNLOAD_URL
    			.replace(BASE_URI_MACRO, getBaseUri())
    			.replace(USER_ID_MACRO, String.valueOf(userId))
    			.replace(USER_DATASET_ID_MACRO, datasetId)
    			.replace(DATAFILE_NAME_MACRO, datafileName);
          callUserDatasetBinaryDownloadService(downloadUrl, trackSourcePath, authCookie, sessionCookie);
    		}
    		catch(WdkModelException wme) {
    		  // Report any error returned by the dataset binary datafile service in the status file.
          manageStatusFile(trackPath, UploadStatus.ERROR, wme.getMessage());
          throw new WdkModelException(wme);
    		}
    		// Set status to completed
        manageStatusFile(trackPath, UploadStatus.COMPLETED, "");
    	  }
	}
    return Response.noContent().build();
  }
  
  @GET
  @Path("user-datasets/{datasetId}/monitor-bigwig-tracks")
  @Produces(MediaType.APPLICATION_JSON)
  public Response monitorTracks(@PathParam("datasetId") String datasetId) throws WdkModelException {
    long userId = getPrivateRegisteredUser().getUserId();
    java.nio.file.Path userTracksDir = GBrowseUtils.getUserTracksDirectory(getWdkModel(), userId);
    if(userTracksDir == null) {
    	  throw new WdkModelException("The user does not have a gbrowse upload tracks directory.");
    }
    JSONArray jsonStatusList = new JSONArray();
    Map<String, GBrowseTrackStatus> tracksStatus = GBrowseUtils.getTracksStatus(userTracksDir);
    for(String trackName : tracksStatus.keySet()) {
    	  GBrowseTrackStatus trackStatus = tracksStatus.get(trackName);
    	  String status = 
    			  trackStatus.getStatusIndicator() +
    			  (UploadStatus.ERROR.name().equals(trackStatus.getStatusIndicator()) ? ": " + trackStatus.getErrorMessage() : "");
    	  jsonStatusList.put(new JSONObject()	  
    			  .put("dataFileName", GBrowseUtils.composeDatafileName(trackName))
    			  .put("status", status));
    }
    return Response.ok(new JSONObject().put("results", jsonStatusList).toString()).build();
  }

  /**
   * Call the binary download service in the user dataset services inventory
   * @param eurl
   * @param trackSourcePath
   * @param authCookie
   * @param sessionCookie
   * @throws WdkModelException
   */
  protected void callUserDatasetBinaryDownloadService(String eurl, java.nio.file.Path trackSourcePath, Cookie authCookie, Cookie sessionCookie) throws WdkModelException {
    Client client = ClientBuilder.newBuilder().build();
    Response response = client
        .target(eurl)
        .property("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
        .request()
        .cookie(new NewCookie(AUTH_COOKIE_NAME,authCookie.getValue()))
        .cookie(new NewCookie(SESSION_COOKIE_NAME,sessionCookie.getValue()))
        .get();
    try {
      if (response.getStatus() == 200) {
        InputStream resultStream = (InputStream) response.getEntity();
        Files.copy(resultStream, trackSourcePath, StandardCopyOption.REPLACE_EXISTING);
        GBrowseUtils.setPosixPermissions(trackSourcePath, GLOBAL_READ_WRITE_PERMS);
      }
      else {
        throw new WdkModelException("Bad http status - " + response.getStatus() + " : " + response.getStatusInfo());
      }
    }
    catch (IOException ioe) {
      throw new WdkModelException(ioe);
    }
    finally {
      response.close();
      client.close();
    }
  }

  /**
   * Create the track status file as needed and populate it with the current status and any error msg.
   * @param trackPath
   * @param status
   * @param msg
   * @throws WdkModelException
   */
  protected void manageStatusFile(String trackPath, UploadStatus status, String msg) throws WdkModelException {
    java.nio.file.Path trackStatusFilePath = Paths.get(trackPath, GBrowseTrackStatus.TRACK_STATUS_FILE_NAME);
    try {
    	  Files.write(trackStatusFilePath, (status + " : " + msg).getBytes(), StandardOpenOption.CREATE);
      GBrowseUtils.setPosixPermissions(trackStatusFilePath, GLOBAL_READ_WRITE_PERMS);
    }  
    catch (IOException ioe) {
      throw new WdkModelException(ioe);
    }
  }
  
  /**
   * Utility method to return the initial portion of the request uri so we can make internal REST calls using
   * that same base uri. The scheme is assumed to be http.
   * 
   * @return - uri string up to app name and service path
   */
  protected String getBaseUri() {
    return getUriInfo().getBaseUri().toString();
  }

}