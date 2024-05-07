package org.apidb.apicommon.service.services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.fgputil.client.TracePropagatingClientInterceptor;
import org.gusdb.fgputil.client.TracingConstants;
import org.gusdb.oauth2.client.OAuthClient;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.service.service.SystemService;
import org.json.JSONObject;

public class ApiSystemService extends SystemService {

  private static final Logger LOG = Logger.getLogger(ApiSystemService.class);

  private static final String PORTAL_PROJECT_ID = "EuPathDB";

  @Override
  protected JSONObject getSeedWdkCachesResponseJson() throws WdkModelException {

    WdkModel wdkModel = getWdkModel();
    boolean aggregate = Boolean.valueOf(getUriInfo().getQueryParameters().getFirst("aggregate")); // default false

    // only return aggregated results if running on the portal AND aggregate parameter is true
    if (!wdkModel.getProjectId().equals(PORTAL_PROJECT_ID) || !aggregate) {
      return super.getSeedWdkCachesResponseJson();
    }

    // collect results from all component sites first, then add portal results
    //    (will pull cached vocabs and public strat results from components)
    ProjectMapper projectMapper = ProjectMapper.getMapper(wdkModel);
    Set<String> projectIds = projectMapper.getFederatedProjects();
    String authHeaderValue = OAuthClient.getAuthorizationHeaderValue(wdkModel.getSystemUserToken());
    ExecutorService exec = Executors.newFixedThreadPool(projectIds.size());
    Map<String,Future<JSONObject>> results = new HashMap<>();
    JSONObject aggregatedResults = new JSONObject();
    try {
      // kick off calls to component sites and fill results map with component JSON
      for (String projectId : projectIds) {
        String webappUrl = projectMapper.getWebAppUrl(projectId);
        LOG.info("Kicking off cache see call to " + webappUrl);
        results.put(projectId, exec.submit(() -> getComponentResults(webappUrl, authHeaderValue)));
      }

      // wait for all calls to complete
      waitForResponses(results);

      // all component calls are complete (for better or worse); start building response from their results
      for (Entry<String,Future<JSONObject>> entry : results.entrySet()) {
        aggregatedResults.put(entry.getKey(), entry.getValue().get());
      }
    }
    catch (InterruptedException e) {
      throw new RuntimeException("Thread was interrupted during processing.  Aborting this request.", e);
    }
    catch (ExecutionException e) {
      throw new RuntimeException("Unable to execute federated call.  Aborting this request.", e);
    }
    finally {
      exec.shutdown();
    }

    // once all component sites' results have been collected, run portal
    LOG.info("Seeding portal cache...");
    aggregatedResults.put(PORTAL_PROJECT_ID, super.getSeedWdkCachesResponseJson());

    return aggregatedResults;
  }

  private static void waitForResponses(Map<String, Future<JSONObject>> futures) throws InterruptedException {
    Map<String,Boolean> returned = futures.keySet().stream().collect(Collectors.toMap(s -> s, s -> false));
    while(true) {
      Thread.sleep(5000);
      boolean allDone = true;
      for (Entry<String,Future<JSONObject>> entry : futures.entrySet()) {
        String projectId = entry.getKey();
        if (entry.getValue().isDone()) {
          if (!returned.get(projectId)) {
            LOG.info("Update: " + projectId + " has returned.");
            returned.put(projectId, true);
          }
        }
        else {
          allDone = false;
        }
      }
      if (allDone) {
        LOG.info("Update: All component sites have returned.");
        break;
      }
    }
  }

  private static JSONObject getComponentResults(String webAppUrl, String authHeaderValue) {
    try (Response response = ClientBuilder
          .newClient()
          .register(new TracePropagatingClientInterceptor(ThreadContext.get(TracingConstants.TRACE_CONTEXT_KEY)))
          .target(webAppUrl + "service/" + CACHE_SEED_ENDPOINT)
          .request()
          .header(HttpHeaders.AUTHORIZATION, authHeaderValue)
          .accept(MediaType.APPLICATION_JSON)
          .buildGet()
          .invoke()) {
      ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
      ((InputStream)response.getEntity()).transferTo(responseBytes);
      String responseBody = new String(responseBytes.toByteArray());
      if (response.getStatus() == 200) {
        return new JSONObject(responseBody);
      }
      else {
        return new JSONObject()
            .put("status", "failed")
            .put("responseStatus", response.getStatus())
            .put("responseBody", responseBody);
      }
    }
    catch (Exception e) {
      return new JSONObject()
          .put("status", "failed")
          .put("exception", e.toString());
    }
  }
}
