package org.apidb.apicommon.service.services;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.view.TranscriptViewHandler;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.service.service.WdkService;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/step/{stepId}/transcriptview")
public class TranscriptToggleService extends WdkService {

  private static final Logger LOG = Logger.getLogger(TranscriptToggleService.class);

  @POST
  public Response setTranscriptFlag(@PathParam("stepId") Integer stepId, String body) {
    try {
      JSONObject input = new JSONObject(body);
      boolean filterTurnedOn = input.getBoolean(TranscriptViewHandler.REPRESENTATIVE_TRANSCRIPT_FILTER);
      Step step = getWdkModel().getStepFactory().getStepById(stepId);
      if (getCurrentUser().getUserId() != step.getUser().getUserId()) {
        return Response.status(Status.FORBIDDEN).entity("Permission Denied").build();
      }
      if (filterTurnedOn) {
        step.addViewFilterOption(TranscriptViewHandler.REPRESENTATIVE_TRANSCRIPT_FILTER, new JSONObject());
      }
      else {
        step.removeViewFilterOption(TranscriptViewHandler.REPRESENTATIVE_TRANSCRIPT_FILTER);
      }
      step.update(false);
      return Response.ok().build();
    }
    catch (WdkModelException e) {
      LOG.error("Unable to update transcript view flag for step " + stepId, e);
      return Response.serverError().build();
    }
    catch (JSONException e) {
      return getBadRequestBodyResponse("JSON object required with property " +
          TranscriptViewHandler.REPRESENTATIVE_TRANSCRIPT_FILTER + " (boolean).");
    }
  }
}
