package org.apidb.apicommon.service.services.jbrowse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.wdk.model.WdkException;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.service.service.AbstractWdkService;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/jbrowse2")
public class JBrowse2Service extends AbstractWdkService {
    private static final String VDI_DATASET_DIR_KEY = "VDI_DATASETS_DIRECTORY";
    private static final String VDI_CONTROL_SCHEMA_KEY ="VDI_CONTROL_SCHEMA";
    private static final String VDI_DATASET_SCHEMA_KEY ="VDI_DATASETS_SCHEMA";
    private static final String WEB_SVC_DIR_KEY ="WEBSERVICEMIRROR";

    @GET
    @Path("orgview/{publicOrganismAbbrev}/config.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseNaTracks(@PathParam("publicOrganismAbbrev") String publicOrganismAbbrev,
                                     @QueryParam("trackSets") String trackSets) throws IOException, WdkException {
        String staticConfigJsonString = getStaticConfigJsonString(publicOrganismAbbrev, trackSets);
        JSONObject staticConfigJson = new JSONObject(staticConfigJsonString);
        JSONArray udTracks = getUserDatasetTracks(publicOrganismAbbrev, trackSets);
        staticConfigJson.getJSONArray("tracks").putAll(udTracks);
        String jsonString = staticConfigJson.toString();
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    JSONArray getUserDatasetTracks(String publicOrganismAbbrev, String tracksString) throws WdkException {
        String buildNumber = getWdkModel().getBuildNumber();
        String projectId = getWdkModel().getProjectId();
        Long userId = getRequestingUser().getUserId();
        String vdiDatasetsDir = getWdkModel().getProperties().get(VDI_DATASET_DIR_KEY);
        String vdiDatasetsSchema = getWdkModel().getProperties().get(VDI_DATASET_SCHEMA_KEY);
        String vdiControlSchema = getWdkModel().getProperties().get(VDI_CONTROL_SCHEMA_KEY);

        String udDataPathString = String.join("/", vdiDatasetsDir, vdiDatasetsSchema, "build-" + buildNumber, projectId);
        JSONArray udTracks = new JSONArray();
        List<String> trackSetList = Arrays.asList(tracksString.split(","));
        if (trackSetList.contains("rnaseq")) {
            udTracks.put(getRnaSeqUdTracks(publicOrganismAbbrev, projectId, vdiControlSchema,
                    udDataPathString, userId));
        }
        return udTracks;
    }

    String getStaticConfigJsonString(String publicOrganismAbbrev, String trackSets) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();

        List<String> command = new ArrayList<>();
        command.add(gusHome + "/bin/jbrowse2config");
        command.add("--orgAbbrev");
        command.add(publicOrganismAbbrev);
        command.add("--projectId");
        command.add(projectId);
        command.add("--buildNumber");
        command.add(buildNumber);
        command.add("--webSvcDir");
        command.add(getWdkModel().getProperties().get(WEB_SVC_DIR_KEY));
        command.add("--trackSets");
        command.add(trackSets);

        return stringFromCommand(command);
    }

    JSONArray getRnaSeqUdTracks(String publicOrganismAbbrev, String projectId, String vdiControlSchema,
                      String udDataPathString, Long userId) throws WdkModelException {

        DataSource appDs = getWdkModel().getAppDb().getDataSource();
        String sql = "select distinct user_dataset_id, name " +
                "from " + vdiControlSchema + ".AvailableUserDatasets aud, " +
                vdiControlSchema + ".dataset_dependency dd " +
                "where project_id = '" + projectId + "' " +
                "and (type = 'RnaSeq' or type = 'BigWig') " +
                "and ((is_public = 1 and is_owner = 1) or user_id = " + userId + ") " +
                "and dd.dataset_id = aud.dataset_id " +
                " dd.identifier = '" + publicOrganismAbbrev + "'";
        try {
            return new SQLRunner(appDs, sql).executeQuery(rs -> {
                JSONArray rnaSeqUdTracks = new JSONArray();
                while (rs.next()) {
                    String datasetId = rs.getString(1);
                    String name = rs.getString(2);
                    List<String> fileNames = getBigwigFileNames(udDataPathString + "/" +datasetId);
                    for (String fileName : fileNames) {
                        rnaSeqUdTracks.put(createBigwigTrackJson(datasetId, name, fileName, publicOrganismAbbrev, udDataPathString));
                    }
                }
                return rnaSeqUdTracks;
            });
        }
        catch (SQLRunnerException e) {
            throw new WdkModelException("Unable to query VDI tables for RNA seq datasets", e.getCause());
        }
    }

    // method written by copilot
    public static List<String> getBigwigFileNames(String directoryPath) throws SQLRunnerException {
        List<String> bwFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".bw")) {
                        bwFiles.add(file.getName());
                    }
                }
            }
        } else {
            throw new SQLRunnerException("User Dataset directory not found for path: " + directoryPath);
        }

        return bwFiles;
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

    String stringFromCommand(List<String> command) throws IOException {
        Process p = processFromCommand(command);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
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
        return pb.start();
    }
}
