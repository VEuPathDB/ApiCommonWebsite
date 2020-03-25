package org.apidb.apicommon.service.services;

import java.util.Arrays;

import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.ProjectService;
import org.json.JSONObject;

public class ApiProjectService extends ProjectService {

  private static final String PROJECT_URLS_KEY = "projectUrls";

  private static final String[] PORTAL_PROJECT_IDS = new String[] { "EuPathDB", "VEuPathDB" };

  @Override
  protected JSONObject addSupplementalProjectInfo(JSONObject projectJson) throws WdkModelException {
    if (Arrays.asList(PORTAL_PROJECT_IDS).contains(getWdkModel().getProjectId())) {
      ProjectMapper mapper = ProjectMapper.getMapper(getWdkModel());
      JSONObject mappedProjects = new JSONObject();
      for (String projectId : mapper.getFederatedProjects()) {
        mappedProjects.put(projectId, mapper.getWebAppUrl(projectId));
      }
      projectJson.put(PROJECT_URLS_KEY, mappedProjects);
    }
    return projectJson;
  }

}
