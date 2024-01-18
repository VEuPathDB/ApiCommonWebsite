package org.apidb.apicommon.service.services.jbrowse;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.apidb.apicommon.service.services.jbrowse.model.JBrowseDatasetResponse;
import org.apidb.apicommon.service.services.jbrowse.model.JBrowseTrack;
import org.apidb.apicommon.service.services.jbrowse.model.VDIDatasetReference;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.errors.ErrorContext.ErrorLocation;
import org.gusdb.wdk.errors.ServerErrorBundle;
import org.gusdb.wdk.events.ErrorEvent;
import org.gusdb.wdk.model.WdkModelException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import org.gusdb.wdk.model.user.dataset.UserDataset;
import org.gusdb.wdk.model.user.dataset.UserDatasetDependency;
import org.gusdb.wdk.model.user.dataset.UserDatasetFile;
import org.gusdb.wdk.model.user.dataset.UserDatasetInfo;
import org.gusdb.wdk.model.user.dataset.UserDatasetMeta;
import org.gusdb.wdk.model.user.dataset.UserDatasetSession;
import org.gusdb.wdk.model.user.dataset.UserDatasetType;
import org.gusdb.wdk.service.formatter.UserDatasetsFormatter;
import org.gusdb.wdk.service.service.user.UserService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gusdb.wdk.service.FileRanges.getFileChunkResponse;
import static org.gusdb.wdk.service.FileRanges.parseRangeHeaderValue;

public class JBrowseUserDatasetsService extends UserService {

  private static final Logger LOG = Logger.getLogger(JBrowseUserDatasetsService.class);
  private static final String VDI_DATASET_DIR_KEY = "VDI_DATASETS_DIRECTORY";
  private static final String VDI_CONTROL_SCHEMA_KEY ="VDI_CONTROL_SCHEMA";

  public JBrowseUserDatasetsService(@PathParam(USER_ID_PATH_PARAM) String uid) {
    super(uid);
  }

  // Need a /data endpoint here to download user dataset files and expose them.

  @GET
  @Path("user-datasets-jbrowse/data")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUserDatasetFileJBrowse(@QueryParam("data") String data,
                                               @HeaderParam("Range") String fileRange) throws WdkModelException {
    String buildNumber = getWdkModel().getBuildNumber();
    String udDir = getWdkModel().getProperties().get("VDI_DATASETS_DIRECTORY");
    // New WDK property: UserDatasetFiles. This is located in /var/www/Common/userDatasets

    // TODO: Validate that the user owns the dataset.

    String path = udDir + "/" + "build-" + buildNumber + "/" +data;

    if (path.contains("..") || path.contains("$")) {
      throw new NotFoundException(formatNotFound("*"));
    }

    return getFileChunkResponse(Paths.get(path), parseRangeHeaderValue(fileRange));
  }


  @GET
  @Path("user-datasets-jbrowse/{organism}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUserDatasetsJBrowse(@PathParam("organism") String publicOrganismAbbrev) {
    LOG.debug("\nservice user-datasets-jbrowse has been called ---gets all jbrowse configuration for user datasets\n");
    final JBrowseDatasetResponse jBrowseDatasetResponse = new JBrowseDatasetResponse();

    try {
      List<VDIDatasetReference> datasets = queryVisibleDatasets(getPrivateRegisteredUser().getUserId());

      List<JBrowseTrack> tracks = datasets.stream()
          .flatMap(dataset -> fetchTracksFromFilesystem(dataset).stream())
          .collect(Collectors.toList());

      jBrowseDatasetResponse.setTracks(tracks);
    }
    // if the user isn't logged in, just return an empty array
    catch (ForbiddenException e) {
      jBrowseDatasetResponse.setTracks(Collections.emptyList());
    }
    // if any other exception occurs, log and send email, but return empty array so UI is not hosed
    catch (Exception e) {
      jBrowseDatasetResponse.setTracks(Collections.emptyList());
        Exception e2 = new WdkModelException("Unable to load JBrowse user datasets for user with ID " +
            getSessionUser().getUserId() + ", organism " + publicOrganismAbbrev, e);
        LOG.error("Could not load JBrowse user datasets", e2);
        Events.trigger(new ErrorEvent(new ServerErrorBundle(e2), getErrorContext(ErrorLocation.WDK_SERVICE)));
    }

    return Response.ok(jBrowseDatasetResponse).build();
  }

  private List<JBrowseTrack> fetchTracksFromFilesystem(VDIDatasetReference vdiDatasetReference) {
    final String vdiDatasetsDir = getWdkModel().getProperties().get(VDI_DATASET_DIR_KEY);
    final String buildNumber = getWdkModel().getBuildNumber();
    final java.nio.file.Path datasetDir = Paths.get(vdiDatasetsDir, "build-" + buildNumber, getWdkModel().getProjectId(), vdiDatasetReference.getId());
    return Arrays.stream(Optional.ofNullable(datasetDir.toFile().listFiles()).orElse(new File[0]))
        .map(bwFile -> {
          final JBrowseTrack jBrowseTrack = new JBrowseTrack();

          jBrowseTrack.setKey(vdiDatasetReference.getName() + " " + bwFile.getName());
          jBrowseTrack.setLabel(vdiDatasetReference.getName() + " " + bwFile.getName());
          jBrowseTrack.setUrlTemplate("/a/service/users/current/user-datasets-jbrowse/data?data="
              + getWdkModel().getProjectId()
              + "/" + vdiDatasetReference.getId()
              + "/" + bwFile.getName());

          JBrowseTrack.Metadata metadata = new JBrowseTrack.Metadata();
          if (vdiDatasetReference.getType().equalsIgnoreCase("BigWig")) {
            jBrowseTrack.setSubcategory("Bigwig Files From User");
            metadata.setSubcategory("Bigwig Files From User");
          }

          if (vdiDatasetReference.getType().equalsIgnoreCase("RNASeq")) {
            jBrowseTrack.setSubcategory("RNASeq");
            metadata.setSubcategory("RNASeq");
          }

          metadata.setDataset(vdiDatasetReference.getName());
          metadata.setMdescription(vdiDatasetReference.getDescription());

          jBrowseTrack.setMetadata(metadata);

          jBrowseTrack.setStyle(new JBrowseTrack.Style());

          return jBrowseTrack;
        })
        .collect(Collectors.toList());
  }

  private List<VDIDatasetReference> queryVisibleDatasets(long userID) {
    final String schema = getWdkModel().getProperties().get(VDI_CONTROL_SCHEMA_KEY);
    String sql = String.format(
        "SELECT user_dataset_id, (SELECT 'BigWig' FROM DUAL) type_name, (SELECT 'name' FROM DUAL) name, (SELECT 'description' FROM DUAL) description FROM %s.dataset_availability da WHERE da.user_id = ?",
        schema.toLowerCase(Locale.ROOT)
    );
    return new SQLRunner(getWdkModel().getAppDb().getDataSource(), sql)
        .executeQuery(new Object[] { userID }, rs -> {
          List<VDIDatasetReference> vdiDatasets = new ArrayList<>();
          while (rs.next()) {
            vdiDatasets.add(datasetFromResultSet(rs));
          }
          return vdiDatasets;
        });
  }

  private VDIDatasetReference datasetFromResultSet(ResultSet resultSet) throws SQLException {
    VDIDatasetReference row = new VDIDatasetReference();
    row.setDescription(resultSet.getString("description"));
    row.setId(resultSet.getString("user_dataset_id"));
    row.setType(resultSet.getString("type_name"));
    row.setDescription(resultSet.getString("description"));
    return row;
  }

  private static class JBrowseUserDatasetFormatter implements UserDatasetsFormatter {

    private final String _publicOrganismAbbrev;

    public JBrowseUserDatasetFormatter(String publicOrganismAbbrev) {
      _publicOrganismAbbrev = publicOrganismAbbrev;
    }

    @Override
    public void addUserDatasetInfoToJsonArray(UserDatasetInfo dataset,
        JSONArray datasetsJson, UserDatasetSession dsSession) throws WdkModelException {
      JSONArray samples = getSamplesJsonForDataset(dataset);
      if (dataset.isInstalled() && dataset.getUserDatasetCompatibility().isCompatible())
	  for (int i = 0 ; i < samples.length(); i++) {
	      datasetsJson.put(samples.getJSONObject(i));
	  }
    }


      private JSONArray getBigwigSampleConfiguration(UserDataset dataset, String subcategory) throws WdkModelException {
          JSONArray samplesJson = new JSONArray();

          int maxScore = 1000;
          Long datasetId = dataset.getUserDatasetId();
          UserDatasetMeta datasetMeta = dataset.getMeta();
          String datasetName = datasetMeta.getName();
          String datasetSummary = datasetMeta.getSummary();

          for (UserDatasetFile file : dataset.getFiles().values()) {
              String fileName = file.getFileName();
              if(!fileName.toUpperCase().endsWith(".BW"))
                  continue ;

              String urlTemplate = "/a/service/users/current/user-datasets/" + datasetId + "/user-datafiles/" + fileName;

              JSONObject json = new JSONObject();
              json.put("storeClass", "JBrowse/Store/SeqFeature/BigWig");
              json.put("urlTemplate", urlTemplate);
              json.put("yScalePosition",  "left");
              json.put("key", datasetName + " " + fileName);
              json.put("label", datasetName + " " + fileName);
              json.put("type", "JBrowse/View/Track/Wiggle/XYPlot");
              json.put("category", "My Data from Galaxy");
              json.put("min_score", 0);
              json.put("max_score", maxScore);

              JSONObject style = new JSONObject();
              style.put("pos_color", "#5B2C6F");

              JSONObject metadata = new JSONObject();
              metadata.put("subcategory", subcategory);
              metadata.put("dataset", datasetName);
              metadata.put("trackType", "Coverage");
              metadata.put("mdescription", datasetSummary);

              json.put("metadata", metadata);
              json.put("style", style);

              samplesJson.put(json);
          }
          return samplesJson;
      }


    private JSONArray getSamplesJsonForDataset(UserDatasetInfo datasetInfo) throws WdkModelException {

      JSONArray samplesJson = new JSONArray();

      String genomeSuffix = "_" + _publicOrganismAbbrev + "_Genome";

      UserDataset dataset = datasetInfo.getDataset();

      boolean matchesOrganismAbbrev = false;

      for (UserDatasetDependency dependency : dataset.getDependencies()) {
        if(dependency.getResourceIdentifier().endsWith(genomeSuffix))
          matchesOrganismAbbrev = true;
      }

      if(!matchesOrganismAbbrev)
        return samplesJson;

      UserDatasetType type = dataset.getType();
      String datasetType = type.getName();

      if(datasetType.equals("RnaSeq")) {
          return getBigwigSampleConfiguration(dataset, "RNASeq");
      }
      if(datasetType.equals("BigwigFiles")) {
          return getBigwigSampleConfiguration(dataset, "Bigwig Files From User");
      }

      return samplesJson;
    }
  }
}
