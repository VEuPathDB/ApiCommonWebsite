package org.apidb.apicommon.service.services.jbrowse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.apidb.apicommon.service.services.jbrowse.model.JBrowseDatasetResponse;
import org.apidb.apicommon.service.services.jbrowse.model.JBrowseTrack;
import org.apidb.apicommon.service.services.jbrowse.model.VDIDatasetReference;
import org.apidb.apicommon.service.services.jbrowse.model.VDIDatasetType;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.errors.ErrorContext.ErrorLocation;
import org.gusdb.wdk.errors.ServerErrorBundle;
import org.gusdb.wdk.events.ErrorEvent;
import org.gusdb.wdk.model.WdkModelException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.gusdb.wdk.service.service.user.UserService;

import java.io.File;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gusdb.wdk.service.FileRanges.getFileChunkResponse;
import static org.gusdb.wdk.service.FileRanges.parseRangeHeaderValue;

public class JBrowseUserDatasetsService extends UserService {

  private static final Logger LOG = Logger.getLogger(JBrowseUserDatasetsService.class);
  private static final String VDI_DATASET_DIR_KEY = "VDI_DATASETS_DIRECTORY";
  private static final String VDI_CONTROL_SCHEMA_KEY ="VDI_CONTROL_SCHEMA";
  private static final String VDI_DATASET_SCHEMA_KEY ="VDI_DATASETS_SCHEMA";

  public JBrowseUserDatasetsService(@PathParam(USER_ID_PATH_PARAM) String uid) {
    super(uid);
  }


  /**
   * This endpoint exposes user dataset track files with the intent of JBrowse using it to retrieve file ranges.
   * Any installed file for a dataset owned by the user can be fetched by this endpoint.
   *
   * URLs to hit this endpoint are constructed by this service when the JBrowse application asks for the user's available
   * tracks.
   */
  @GET
  @Path("user-datasets-jbrowse/data")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllUserDatasetFileJBrowse(@QueryParam("data") String data,
                                               @QueryParam("datasetID") String datasetID,
                                               @HeaderParam("Range") String fileRange) throws WdkModelException {
    String buildNumber = getWdkModel().getBuildNumber();
    String udDir = getWdkModel().getProperties().get(VDI_DATASET_DIR_KEY);
    String schemaKey = getWdkModel().getProperties().get(VDI_DATASET_SCHEMA_KEY).toLowerCase(Locale.ROOT);

    // Verify that the dataset belongs to user. Random people should not be able to download anyone's files, even
    // though they are protected by obscurity.
    if (!datasetBelongsToUser(getPrivateRegisteredUser().getUserId(), datasetID)) {
      throw new NotFoundException("Unable to find dataset with ID " + datasetID);
    }

    String path = String.format("%s/%s/build-%s/%s/%s/%s", udDir, schemaKey, buildNumber, getWdkModel().getProjectId(), datasetID, data);

    LOG.info("Looking up jbrowse files for dataset {} in directory " + path);

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
      LOG.info("Found " + datasets.size() + " datasets for user " + getPrivateRegisteredUser().getUserId());

      // Any tracks that are in the installed dataset dir on the filesystem are installed in this project, fetch
      // them indiscriminately!
      List<JBrowseTrack> tracks = datasets.stream()
          .flatMap(dataset -> fetchTracksFromFilesystem(dataset).stream())
          .collect(Collectors.toList());

      LOG.info("Found " + tracks.size() + " tracks for user " + getPrivateRegisteredUser().getUserId());
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
            getRequestingUser().getUserId() + ", organism " + publicOrganismAbbrev, e);
        LOG.error("Could not load JBrowse user datasets", e2);
        Events.trigger(new ErrorEvent(new ServerErrorBundle(e2), getErrorContext(ErrorLocation.WDK_SERVICE)));
    }

    return Response.ok(jBrowseDatasetResponse).build();
  }

  /**
   * Fetch all tracks from the filesystem for a given dataset reference. If the dataset has no installed files in this
   * project an empty list is returned.
   */
  private List<JBrowseTrack> fetchTracksFromFilesystem(VDIDatasetReference vdiDatasetReference) {
    final String vdiDatasetsDir = getWdkModel().getProperties().get(VDI_DATASET_DIR_KEY);
    String schemaKey = getWdkModel().getProperties().get(VDI_DATASET_SCHEMA_KEY).toLowerCase(Locale.ROOT);
    final String buildNumber = getWdkModel().getBuildNumber();
    final java.nio.file.Path datasetDir = Paths.get(vdiDatasetsDir, schemaKey, "build-" + buildNumber, getWdkModel().getProjectId(), vdiDatasetReference.getId());
    LOG.info("Looking for tracks in " + datasetDir);
    return Arrays.stream(Optional.ofNullable(datasetDir.toFile().listFiles()).orElse(new File[0]))
        .map(jbrowseFile -> {
          final JBrowseTrack jBrowseTrack = new JBrowseTrack();

          jBrowseTrack.setKey(vdiDatasetReference.getName() + " " + jbrowseFile.getName());
          jBrowseTrack.setLabel(vdiDatasetReference.getName() + " " + jbrowseFile.getName());
          jBrowseTrack.setUrlTemplate(String.format("/a/service/users/current/user-datasets-jbrowse/data?datasetID=%s&data=%s",
              vdiDatasetReference.getId(), jbrowseFile.getName()));

          JBrowseTrack.Metadata metadata = new JBrowseTrack.Metadata();

          final String subCategory = VDIDatasetType.fromVDIName(vdiDatasetReference.getType()).getJbrowseSubcategoryName();
          jBrowseTrack.setSubcategory(subCategory);
          metadata.setSubcategory(subCategory);

          metadata.setDataset(vdiDatasetReference.getName());
          metadata.setMdescription(vdiDatasetReference.getDescription());

          jBrowseTrack.setMetadata(metadata);
          jBrowseTrack.setStyle(new JBrowseTrack.Style());

          return jBrowseTrack;
        })
        .collect(Collectors.toList());
  }

  private boolean datasetBelongsToUser(long userID, String datasetID) {
    final String schema = getWdkModel().getProperties().get(VDI_CONTROL_SCHEMA_KEY);
    String sql = String.format(
        "SELECT user_dataset_id FROM %s.AvailableUserDatasets da WHERE da.user_id = ? AND da.user_dataset_id = ?",
        schema.toLowerCase(Locale.ROOT)
    );
    return new SQLRunner(getWdkModel().getAppDb().getDataSource(), sql)
        .executeQuery(new Object[] { userID, datasetID }, ResultSet::next);
  }


  /**
   * Query the visible datasets according the VDI control schema's visible datasets view.
   *
   * Note that this view is specifically designed and intended for use by applications to know which datasets are available
   * to a particular user.
   *
   * @param userID UserID to retrieve visible datasets for.
   * @return List of visible datasets.
   */
  private List<VDIDatasetReference> queryVisibleDatasets(long userID) {
    final String schema = getWdkModel().getProperties().get(VDI_CONTROL_SCHEMA_KEY);
    String sql = String.format(
        "SELECT user_dataset_id, type, name, description FROM %s.AvailableUserDatasets da WHERE da.user_id = ?",
        schema
    );
    LOG.debug("Querying visible datasets: " + sql);
    return new SQLRunner(getWdkModel().getAppDb().getDataSource(), sql)
        .executeQuery(new Object[] { userID }, rs -> {
          List<VDIDatasetReference> vdiDatasets = new ArrayList<>();
          while (rs.next()) {
            vdiDatasets.add(datasetFromResultSet(rs));
          }
          return vdiDatasets;
        });
  }

  /**
   * Constructs a {@link VDIDatasetReference} from a ResultSet produced by a query of the visible datasets view.
   */
  private VDIDatasetReference datasetFromResultSet(ResultSet resultSet) throws SQLException {
    VDIDatasetReference row = new VDIDatasetReference();
    row.setDescription(resultSet.getString("description"));
    row.setId(resultSet.getString("user_dataset_id"));
    row.setType(resultSet.getString("type"));
    row.setDescription(resultSet.getString("description"));
    row.setName(resultSet.getString("name"));
    return row;
  }
}
