package org.apidb.apicommon.service.services.jbrowse;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.BadRequestException;

import org.apache.log4j.Logger;
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

    private static final Logger LOG = Logger.getLogger(JBrowse2Service.class);

    private static final String VDI_DATASETS_DIRECTORY_KEY ="VDI_DATASETS_DIRECTORY";
    private static final String VDI_CONTROL_SCHEMA_KEY ="VDI_CONTROL_SCHEMA";
    private static final String VDI_DATASET_SCHEMA_KEY ="VDI_DATASETS_SCHEMA";
    private static final String WEB_SVC_DIR_KEY ="WEBSERVICEMIRROR";

    private static final String SVC_USER_DATASETS_DIR = "./userDatasetsData";  // hard-coded mount point in the jbrowse2 service
    /*
    Get config for a single organism.  Assumes JSON will easily fit in memory.
     */
    @GET
    @Path("orgview/{publicOrganismAbbrev}/config.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseSingleOrgTracks(@PathParam("publicOrganismAbbrev") String publicOrganismAbbrev,
                                     @QueryParam("trackSets") String trackSetsString) throws IOException, WdkException {

        String errMsg = "Must provide a comma delimited list of tracks in a 'trackSets' query param";
        if (trackSetsString == null) throw new BadRequestException(errMsg);
        List<String> trackSetsList = Arrays.asList(trackSetsString.split(","));
        if (trackSetsList.isEmpty()) throw new BadRequestException(errMsg);
        
        // get static json config, for this organism and set of tracks
        String staticConfigJsonString = getStaticConfigJsonString(publicOrganismAbbrev, trackSetsString);
        JSONObject staticConfigJson = new JSONObject(staticConfigJsonString);

        // get similar from user datasets
        JSONArray udTracks = getUserDatasetTracks(publicOrganismAbbrev, trackSetsList);

        // merge UD tracks into static
        staticConfigJson.getJSONArray("tracks").putAll(udTracks);

        // send response
        String jsonString = staticConfigJson.toString();
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    // call out to perl code to produce static config json
    String getStaticConfigJsonString(String publicOrganismAbbrev, String trackSetsString) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();

        List<String> command = new ArrayList<>();
        command.add(gusHome + "/bin/jbrowse2Config");
        command.add("--orgAbbrev");
        command.add(publicOrganismAbbrev);
        command.add("--projectId");
        command.add(projectId);
        command.add("--buildNumber");
        command.add(buildNumber);
        command.add("--webSvcDir");
        command.add(getWdkModel().getProperties().get(WEB_SVC_DIR_KEY));
        command.add("--trackSets");
        command.add(trackSetsString);

        return stringFromCommand(command);
    }

    JSONArray getUserDatasetTracks(String publicOrganismAbbrev, List<String> trackSetList) throws WdkModelException {
        String buildNumber = getWdkModel().getBuildNumber();
        String projectId = getWdkModel().getProjectId();
        Long userId = getRequestingUser().getUserId();
        String vdiDatasetsSchema = getWdkModel().getProperties().get(VDI_DATASET_SCHEMA_KEY);
        String vdiControlSchema = getWdkModel().getProperties().get(VDI_CONTROL_SCHEMA_KEY);
        String vdiDatasetsDir = getWdkModel().getProperties().get(VDI_DATASETS_DIRECTORY_KEY);

        String path = String.join("/", vdiDatasetsSchema.toLowerCase(), "build-" + buildNumber, projectId);
        String svcUserDataPathString = SVC_USER_DATASETS_DIR + "/" + path;
        String wdkUserDatasetsPathString = vdiDatasetsDir + "/" + path;
        JSONArray udTracks = new JSONArray();

        // for now we only have rnaseq UD tracks
        if (trackSetList.contains("rnaseq")) {
            udTracks.put(getRnaSeqUdTracks(publicOrganismAbbrev, projectId, vdiControlSchema, wdkUserDatasetsPathString,
                    svcUserDataPathString, userId));
        }
        return udTracks;
    }

    JSONArray getRnaSeqUdTracks(String publicOrganismAbbrev, String projectId, String vdiControlSchema,
                      String wdkUserDatasetsPathString, String svcUserDatasetsPathString, Long userId) throws WdkModelException {

        DataSource appDs = getWdkModel().getAppDb().getDataSource();
        String sql = "select distinct user_dataset_id, name " +
                "from " + vdiControlSchema + ".AvailableUserDatasets aud, " +
                vdiControlSchema + ".dataset_dependency dd " +
                "where project_id = '" + projectId + "' " +
                "and (type = 'rnaseq' or type = 'bigwigfiles') " +
                "and ((is_public = 1 and is_owner = 1) or user_id = " + userId + ") " +
                "and dd.dataset_id = aud.user_dataset_id " +
                "and dd.identifier = '" + publicOrganismAbbrev + "'";

        try {
            return new SQLRunner(appDs, sql).executeQuery(rs -> {
                JSONArray rnaSeqUdTracks = new JSONArray();
                while (rs.next()) {
                    String datasetId = rs.getString(1);
                    String name = rs.getString(2);
                    JSONObject track = createBigwigTrackJson(datasetId, name, publicOrganismAbbrev);
                    rnaSeqUdTracks.put(track);
                    List<String> fileNames = getBigwigFileNames(wdkUserDatasetsPathString + "/" + datasetId);
                    for (String fileName : fileNames) {

                      track.getJSONObject("adapter")
                                .getJSONArray("subadapters")
                                .put(createBigwigSubadapterJson(datasetId, fileName, svcUserDatasetsPathString));
                    }
                }
                return rnaSeqUdTracks;
            });
        }
        catch (SQLRunnerException e) {
          throw new WdkModelException("Unable to query VDI tables for RNA seq datasets. " + e.getMessage(), e.getCause());
        }
    }

    // boilerplate method written by copilot
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
    JSONObject createBigwigTrackJson(String vdiId, String vdiName, String organismAbbrev) {
        return new JSONObject()
            .put("assemblyNames", new JSONArray().put(organismAbbrev))
            .put("trackId", vdiId)
            .put("name", vdiName)
            .put("displays", new JSONArray()
                .put(new JSONObject()
                    .put("displayId", "wiggle_ApiCommonModel::Model::JBrowseTrackConfig::MultiBigWigTrackConfig::XY=HASH(0x2249320)")
                    .put("maxScore", 1)
                    .put("maxScore", 1000)
                    .put("defaultRendering", "multirowxy")
                    .put("type", "MultiLinearWiggleDisplay")
                    .put("scaleType", "log")
                    )
                )
            .put("adapter", new JSONObject()
                    .put("subadapters", new JSONArray())
            );
    }

    JSONObject createBigwigSubadapterJson(String vdiId, String fileName, String userDatasetsFilePath) {
        JSONObject subAdapter = new JSONObject();
        subAdapter.put("color1", "grey");
        subAdapter.put("name", fileName);
        subAdapter.put("type", "BigWigAdapter");
        JSONObject location = new JSONObject().put("locationType", "UriLocation");
        location.put("uri", String.join("/", userDatasetsFilePath, vdiId, fileName));
        subAdapter.put("bigWigLocation", location);
        return subAdapter;
    }

    String stringFromCommand(List<String> command) throws IOException {
      LOG.debug("Running command: " + String.join(" ", command));
        try {
            Process p = processFromCommand(command);

            ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();
            p.getErrorStream().transferTo(stringBuffer);
            String errors = stringBuffer.toString();

            stringBuffer.reset();
            p.getInputStream().transferTo(stringBuffer);

            if (p.waitFor() != 0) {
                throw new RuntimeException("Subprocess from [" + String.join(" ", command) + "] returned non-zero.  Errors:\n" + errors);
            }

            return stringBuffer.toString();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Subprocess from [" + String.join(" ", command) + "] was interrupted befor it could complete.");
        }
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
