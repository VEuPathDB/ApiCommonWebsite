package org.apidb.apicommon.service.services;

import javax.ws.rs.Path;

import org.gusdb.wdk.service.service.WdkService;
//import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;

@Path("/step/{stepId}/transcript-view/config")
public class TranscriptToggleService extends WdkService {
/*
  private static final Logger LOG = Logger.getLogger(TranscriptToggleService.class);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response setTranscriptFlag(@PathParam("stepId") Integer stepId, String body) {
    try {
      LOG.info("Recieved transcript toggle POST request for step ID " + stepId + " and body " + body);
      JSONObject input = new JSONObject(body);
      boolean filterTurnedOn = input.getBoolean(RepresentativeTranscriptFilter.FILTER_NAME);
      LOG.info("Action is to turn filter: " + filterTurnedOn);
      Step step = getWdkModel().getStepFactory().getStepById(stepId);
      if (getCurrentUser().getUserId() != step.getUser().getUserId()) {
        return Response.status(Status.FORBIDDEN).entity("Permission Denied").build();
      }
      if (filterTurnedOn) {
        step.addViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME, new JSONObject());
      }
      else {
        step.removeViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME);
      }
      step.saveParamFilters();
      return Response.ok().build();
    }
    catch (WdkModelException e) {
      LOG.error("Unable to update transcript view flag for step " + stepId, e);
      return Response.serverError().build();
    }
    catch (JSONException e) {
      LOG.error("Unable to parse input JSON", e);
      return getBadRequestBodyResponse("JSON object required with property " +
          RepresentativeTranscriptFilter.FILTER_NAME + " (boolean).");
    }
  }
  */
}
