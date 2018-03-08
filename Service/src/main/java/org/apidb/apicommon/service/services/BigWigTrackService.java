package org.apidb.apicommon.service.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apidb.apicommon.model.gbrowse.GBrowseUtils;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.user.UserService;

public class BigWigTrackService extends UserService {
  private static final String TRACK_SOURCE_PATH_MACRO = "$$TRACK_SOURCE_PATH_MACROS$$";
  private static final String TRACK_NAME_MACRO = "$$TRACK_NAME_MACROS$$";
  private static final String GLOBAL_READ_WRITE_PERMS = "rw-rw-rw-";
  
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

  @GET
  @Path("load-bigwig-track")
  public Response loadTrack(@CookieParam("wdk_check_auth") NewCookie authCookie,
		  @CookieParam("JSESSIONID") NewCookie sessionCookie,
		  @QueryParam("eurl") String eurl) throws WdkModelException {
	String irodsUrl = FormatUtil.urlDecodeUtf8(eurl);
	String trackName = GBrowseUtils.composeTrackName(irodsUrl);

	long userId = getPrivateRegisteredUser().getUserId();
	java.nio.file.Path userTracksDir = GBrowseUtils.getUserTracksDirectory(getWdkModel(), userId);
	if(userTracksDir != null) {
    	  Set<String> persistedTracks = GBrowseUtils.getPersistedTracks(userTracksDir).keySet();
    	  // If track already exists, do nothing.
    	  if(!persistedTracks.contains(trackName)) {
    		String trackPath = Paths.get(userTracksDir.toString(), trackName).toString();
    		java.nio.file.Path trackSourcePath = Paths.get(trackPath, "SOURCES", trackName );
    		try {
    		  IoUtil.createOpenPermsDirectory(Paths.get(trackPath));
    		  IoUtil.createOpenPermsDirectory(Paths.get(trackPath, "SOURCES"));
          String configurationFileData = CONFIGURATION_TEMPLATE
        		.replace(TRACK_SOURCE_PATH_MACRO, trackSourcePath.toString())
        		.replace(TRACK_NAME_MACRO, trackName);
          java.nio.file.Path configurationFilePath = Paths.get(trackPath, trackName + ".conf");
          Files.write(configurationFilePath, configurationFileData.getBytes());
          GBrowseUtils.setPosixPermissions(configurationFilePath, GLOBAL_READ_WRITE_PERMS);
        }
        catch(IOException ioe) {
        	  throw new WdkModelException("Unable to create the custom track: " + trackName, ioe);
        }
        callUserDatasetBinaryDownloadService(eurl, trackSourcePath, authCookie, sessionCookie);
    	  }
	}
    return Response.noContent().build();
  }
  
  protected void callUserDatasetBinaryDownloadService(String eurl, java.nio.file.Path trackSourcePath, NewCookie authCookie, NewCookie sessionCookie) throws WdkModelException {
    Client client = ClientBuilder.newBuilder().build();
    Response response = client
        .target(eurl)
        .property("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
        .request()
        .cookie(authCookie)
        .cookie(sessionCookie)
        .get();
    try {
      if (response.getStatus() == 200) {
        InputStream resultStream = (InputStream) response.getEntity();
        Files.copy(resultStream, trackSourcePath, StandardCopyOption.REPLACE_EXISTING);
        GBrowseUtils.setPosixPermissions(trackSourcePath, GLOBAL_READ_WRITE_PERMS);
      }
      else {
        throw new WdkModelException("Bad status - " + response.getStatus());
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

}
