package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.FormatUtil.NL;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.eupathdb.common.model.MultiBlastServiceUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.CloseableResponse;
import org.gusdb.fgputil.events.Events;
import org.gusdb.wdk.errors.ErrorContext.ErrorLocation;
import org.gusdb.wdk.errors.ServerErrorBundle;
import org.gusdb.wdk.events.ErrorEvent;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.SessionService;
import org.json.JSONObject;

public class ApiSessionService extends SessionService {

  private static final Logger LOG = Logger.getLogger(ApiSessionService.class);

  @Override
  protected void transferOwnership(User oldUser, User newUser, WdkModel wdkModel) throws WdkModelException {

    // transfer strategies and datasets
    super.transferOwnership(oldUser, newUser, wdkModel);

    // also transfer multi-blast jobs
    transferMultiBlastJobs(oldUser, newUser, wdkModel);
  }

  /**
   * Transfers multi-blast jobs from the guest user to the newly signed-in user
   * by calling the link-guest endpoint on the configured multi-blast service.
   * 
   * @param oldUser guest user
   * @param newUser registered user
   * @param wdkModel model
   * @throws WdkModelException if no mblast service is configured (error calling service is not fatal)
   */
  private void transferMultiBlastJobs(User oldUser, User newUser, WdkModel wdkModel) throws WdkModelException {

    // get multi-blast URL from the model; this is a fatal operation, even though the actual merge is not
    String mblastServiceUrl = MultiBlastServiceUtil.getMultiBlastServiceUrl(wdkModel, e -> new WdkModelException(e));
    String jobMergerUrl = mblastServiceUrl + "/link-guest";

    // request body to merge guest's jobs to newly logged in user
    String body = new JSONObject()
        .put("guestID", oldUser.getUserId())
        .toString();

    // auth header for new user
    TwoTuple<String,String> authHeader = MultiBlastServiceUtil.getAuthHeader(wdkModel, newUser);

    LOG.debug("Making request to copy mblast jobs:" + NL +
        "POST to " + jobMergerUrl + NL +
        "Header: " + authHeader.getKey() + ":" + authHeader.getValue() + NL +
        "Body: " + body);

    // make request and check result
    try (CloseableResponse response = new CloseableResponse(
        ClientBuilder.newClient()
          .target(jobMergerUrl)
          .request("*/*")
          .header(authHeader.getKey(), authHeader.getValue())
          .post(Entity.entity(body, MediaType.APPLICATION_JSON)))) {

      // this is a non-fatal error (should not keep user from logging in), but make every effort to alert QA
      if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        String error = ClientUtil.readSmallResponseBody(response);
        handleMultiBlastException(new WdkModelException(
            "Unable to merge multi-blast jobs from guest user " +
            oldUser.getUserId() + " to registered user " + newUser.getUserId() +
            ". Service at " + jobMergerUrl + " returned " + response.getStatus() +
            " with error: " + error));
      }
    }
    catch (Exception e) {
      handleMultiBlastException(e);
    }
  }

  private void handleMultiBlastException(Exception e) {
    LOG.error("Multi-blast merge jobs request failed", e);
    Events.trigger(new ErrorEvent(
        new ServerErrorBundle(e),
        getErrorContext(ErrorLocation.WDK_SERVICE)));
  }
}
