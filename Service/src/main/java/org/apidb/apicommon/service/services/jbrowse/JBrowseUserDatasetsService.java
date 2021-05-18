package org.apidb.apicommon.service.services.jbrowse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.errors.ErrorContext.ErrorLocation;
import org.gusdb.wdk.errors.ServerErrorBundle;
import org.gusdb.wdk.events.ErrorEvent;
import org.gusdb.wdk.model.WdkModelException;
import javax.ws.rs.ForbiddenException;
import org.gusdb.wdk.model.user.dataset.UserDataset;
import org.gusdb.wdk.model.user.dataset.UserDatasetDependency;
import org.gusdb.wdk.model.user.dataset.UserDatasetFile;
import org.gusdb.wdk.model.user.dataset.UserDatasetInfo;
import org.gusdb.wdk.model.user.dataset.UserDatasetMeta;
import org.gusdb.wdk.model.user.dataset.UserDatasetSession;
import org.gusdb.wdk.model.user.dataset.UserDatasetType;
import org.gusdb.wdk.service.formatter.UserDatasetsFormatter;
import org.gusdb.wdk.service.service.user.UserDatasetService;
import org.gusdb.wdk.service.service.user.UserService;
import org.json.JSONArray;
import org.json.JSONObject;

public class JBrowseUserDatasetsService extends UserService {

  private static final Logger LOG = Logger.getLogger(JBrowseUserDatasetsService.class);

  public JBrowseUserDatasetsService(@PathParam(USER_ID_PATH_PARAM) String uid) {
    super(uid);
  }

  @GET
  @Path("user-datasets-jbrowse/{organism}")
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getAllUserDatasetsJBrowse(@PathParam("organism") String publicOrganismAbbrev) {

    LOG.debug("\nservice user-datasets-jbrowse has been called ---gets all jbrowse configuration for user datasets\n");

    JSONArray tracks;
    try {
        tracks = UserDatasetService.getAllUserDatasetsJson(getWdkModel(),
                                                           getPrivateRegisteredUser(),
                                                           new JBrowseUserDatasetFormatter(publicOrganismAbbrev));
    }
    // if the user isn't logged in, just return an empty array
    catch (ForbiddenException e) {
        tracks = new JSONArray();
    }
    // if any other exception occurs, log and send email, but return empty array so UI is not hosed
    catch (Exception e) {
        tracks = new JSONArray();
        Exception e2 = new WdkModelException("Unable to load JBrowse user datasets for user with ID " +
            getSessionUser().getUserId() + ", organism " + publicOrganismAbbrev, e);
        LOG.error("Could not load JBrowse user datasets", e2);
        Events.trigger(new ErrorEvent(new ServerErrorBundle(e2), getErrorContext(ErrorLocation.WDK_SERVICE)));
    }

    return new JSONObject().put("tracks", tracks);
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
