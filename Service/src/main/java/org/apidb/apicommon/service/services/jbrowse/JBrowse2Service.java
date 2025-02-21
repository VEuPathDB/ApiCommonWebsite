package org.apidb.apicommon.service.services.jbrowse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.service.service.AbstractWdkService;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/jbrowse2")
public class JBrowse2Service extends AbstractWdkService {

    public static String appType = "jbrowse2";

    @GET
    @Path("{tracks}/{publicOrganismAbbrev}/{aaOrNa}/config.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseTracks(@PathParam("publicOrganismAbbrev") String publicOrganismAbbrev,
                                     @PathParam("aaOrNa") String aaOrNa,
                                     @PathParam("trackSets") String trackSets) throws IOException {

        boolean isPbrowse = aaOrNa.equals("aa");
        String staticConfigJsonString = getStaticConfigJsonString(publicOrganismAbbrev, isPbrowse, trackSets);
        JSONObject staticConfigJson = new JSONObject(staticConfigJsonString);
        JSONArray udTracks = getUserDatasetTracks(publicOrganismAbbrev, isPbrowse, trackSets);
        staticConfigJson.getJSONArray("tracks").putAll(udTracks);
        String jsonString = staticConfigJson.toString();
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    JSONArray getUserDatasetTracks(String publicOrganismAbbrev, Boolean isPbrowse, String tracksString) {
        String buildNumber = getWdkModel().getBuildNumber();
        String projectId = getWdkModel().getProjectId();
        Long userId = getRequestingUser().getUserId();
        String vdiDatasetsDir = getWdkModel().getProperties().get("VDI_DATASETS_DIRECTORY");
        String vdiDatasetsSchema = getWdkModel().getProperties().get("VDI_DATASETS_DIRECTORY");
        String vdiControlSchema = getWdkModel().getProperties().get("VDI_CONTROL_DIRECTORY");


/*
VDI_CONTROL_SCHEMA=VDI_CONTROL_DEV_N
VDI_DATASETS_DIRECTORY=/var/www/Common/userDatasets
VDI_DATASETS_SCHEMA=VDI_DATASETS_DEV_N
/var/www/Common/userDatasets/vdi_datasets_dev_n/build-68/PlasmoDB/
 */
        String udDataPathString = String.join("/", vdiDatasetsDir, vdiDatasetsSchema, "build-" + buildNumber, projectId);
        JSONArray udTracks = new JSONArray();
        List<String> trackSetList = Arrays.asList(tracksString.split(","));
        if (trackSetList.contains("rnaseq")) {
            udTracks.put(getRnaSeqUdTracks(publicOrganismAbbrev, projectId, vdiControlSchema, vdiDatasetsSchema,
                    udDataPathString, userId));
        }
        return null;
    }

    String getStaticConfigJsonString(String publicOrganismAbbrev, boolean isPbrowse, String tracks) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseTracks");
        command.add(publicOrganismAbbrev);
        command.add(projectId);
        command.add(String.valueOf(isPbrowse));
        command.add(tracks);
        command.add(appType);
        command.add("trackListOnly");

        return stringFromCommand(command);
    }

    JSONArray getRnaSeqUdTracks(String publicOrganismAbbrev, String projectId, String vdiControlSchema, String vdiDatasetsSchema,
                      String udDataPathString, Long userId) {

        DataSource appDs = getWdkModel().getAppDb().getDataSource();
        String sql = "select distinct user_dataset_id, name " +
                "from " + vdiControlSchema + ".AvailableUserDatasets " +
                "where project_id = '" + projectId + "' " +
                "and (type = 'RnaSeq' or type = 'BigWig') " +
                "and ((is_public = 1 and is_owner = 1) or user_id = " + userId + ")";
        try {
            List<UserDatasetInfo> userDatasetInfos =  new SQLRunner(appDs, sql).executeQuery(rs -> {
                List<UserDatasetInfo> udi = new ArrayList<>();
                while (rs.next()) {
                    String datasetId = rs.getString(1);
                    String name = rs.getString(2);
                    udi.add(new UserDatasetInfo(datasetId, name));
                }
                return udi;
            });
            
        }
        catch (SQLRunnerException e) {
            throw new PluginModelException("Unable to generate project ID map for organism doc type", e.getCause());
        }
    }

/*
    MULTI BIGWIG TRACK EXAMPLE
    {
      "assemblyNames": [
        "ORG_ABBREV"
      ],
      "trackId": "VDI_ID",
      "name": "VDI_NAME",
      "displays": [
        {
          "displayId": "wiggle_ApiCommonModel::Model::JBrowseTrackConfig::MultiBigWigTrackConfig::XY=HASH(0x2249320)",
          "maxScore": 1000,
          "minScore": 1,
          "defaultRendering": "multirowxy",
          "type": "MultiLinearWiggleDisplay",
          "scaleType": "log"
        }
      ],
      "adapter": {
        "subadapters": [
          {
            "color": "grey",
            "name": "FILE_NAME",
            "type": "BigWigAdapter",
            "bigWigLocation": {
              "locationType": "UriLocation",
              "uri": "USER_DATASET_PATH/VDI_ID/FILE_NAME"
            }
          }
        }
      }
 */
    JSONObject createBigwigTrackJson(String vdiId, String vdiName, String fileName, String organismAbbrev, String userDatasetsFilePath) {
        JSONObject track = new JSONObject();
        track.put("assemblyNames", new JSONArray().put(organismAbbrev));
        track.put("trackId", vdiId);
        track.put("name", vdiName);
        JSONObject display = new JSONObject();
        display.put("displayId", "wiggle_ApiCommonModel::Model::JBrowseTrackConfig::MultiBigWigTrackConfig::XY=HASH(0x2249320)");
        display.put("maxScore", 1);
        display.put("maxScore", 1000);
        display.put("defaultRendering", "multirowxy");
        display.put("type", "MultiLinearWiggleDisplay");
        display.put("scaleType", "log");
        JSONArray displays = new JSONArray().put(display);
        track.put("displays", displays);
        JSONObject subAdapter = new JSONObject();
        subAdapter.put("color1", "grey");
        subAdapter.put("name", fileName);
        subAdapter.put("type", "BigWigAdapter");
        JSONObject location = new JSONObject().put("locationType", "UriLocation");
        location.put("uri", String.join("/", userDatasetsFilePath, vdiId, fileName));
        subAdapter.put("bigWigLocation", location);
        return track;
    }

    class UserDatasetInfo {
        UserDatasetInfo(String id, String name) {
            vdiId = id;
            vdiName = name;
        }
        String vdiId;
        String vdiName;
        List<String> fileNames;
    }

    List<UserDatasetInfo> getUserDatasetInfo() {
        return new ArrayList<UserDatasetInfo>();
    }

    String stringFromCommand(List<String> command) throws IOException {
        Process p = processFromCommand(command);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    Process processFromCommand (List<String> command) throws IOException {
        for (int i = 0; i < command.size(); i++) {
            if (command.get(i) == null)
                throw new WdkRuntimeException(
                        "Command part at index " + i + " is null.  Could be due to unchecked user input.");
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("GUS_HOME", getWdkModel().getGusHome());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        return p;
    }
}
