package org.apidb.apicommon.service.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
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
  public JSONObject getAllUserDatasetsJBrowse(@PathParam("organism") String publicOrganismAbbrev) throws WdkModelException {

    LOG.debug("\nservice user-datasets-jbrowse has been called ---gets all jbrowse configuration for user datasets\n");

    JSONArray tracks = UserDatasetService.getAllUserDatasetsJson(
        getWdkModel(),
        getPrivateRegisteredUser(),
        new JBrowseUserDatasetFormatter(publicOrganismAbbrev));

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
      JSONArray samples = getSamplesJsonForDataset(dsSession, dataset);
      for (int i = 0 ; i < samples.length(); i++) {
        datasetsJson.put(samples.getJSONObject(i));
      }
    }

    private JSONArray getSamplesJsonForDataset(UserDatasetSession dsSession, UserDatasetInfo datasetInfo) throws WdkModelException {

      JSONArray samplesJson = new JSONArray();

      String genomeSuffix = "_" + _publicOrganismAbbrev + "_Genome";
      int maxScore = 1000;

      UserDataset dataset = datasetInfo.getDataset();
      UserDatasetType type = dataset.getType();
      String datasetType = type.getName();
      if(datasetType.equals("RnaSeq"))
        datasetType = "RNASeq";

      boolean matchesOrganismAbbrev = false;

      for (UserDatasetDependency dependency : dataset.getDependencies()) {
        if(dependency.getResourceIdentifier().endsWith(genomeSuffix)) 
          matchesOrganismAbbrev = true;
      }

      if(!matchesOrganismAbbrev) 
        return samplesJson;


      Long datasetId = dataset.getUserDatasetId();
      UserDatasetMeta datasetMeta = dataset.getMeta();
      String datasetName = datasetMeta.getName();
      String datasetSummary = datasetMeta.getSummary();

      for (UserDatasetFile file : dataset.getFiles().values()) {
        String fileName = file.getFileName(dsSession);
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
        json.put("min_score",0);
        json.put("max_score", maxScore);

        JSONObject metadata = new JSONObject();
        metadata.put("subcategory", datasetType);
        metadata.put("dataset", datasetName);
        metadata.put("trackType", "Coverage");
        metadata.put("mdescription", datasetSummary);

        json.put("metadata", metadata);

        samplesJson.put(json);
      }

      return samplesJson;
    }
  }
}
