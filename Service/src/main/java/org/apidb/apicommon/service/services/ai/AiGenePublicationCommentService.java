package org.apidb.apicommon.service.services.ai;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apidb.apicommon.service.services.comments.AbstractUserCommentService;
import org.gusdb.wdk.model.WdkModelException;
import org.json.JSONObject;

/**
 * AI-assisted gene-publication summary service. A user supplies a gene
 * ({@code gene_id}) and a publication (a PubMed id, or paper text the front-end
 * extracted from an uploaded PDF). The service resolves the publication text,
 * verifies the gene is mentioned, runs an LLM to summarise the gene's function,
 * and persists a {@code user_comment} carrying AI provenance.
 *
 * <p>Three endpoints, matching the front-end contract
 * ({@code CLAUDE-ai-user-comments.md}). JSON-only end to end. The POST runs a
 * synchronous prelude then spawns an async pipeline; GET polls job status;
 * DELETE cancels. Extends {@link AbstractUserCommentService} to reuse
 * {@code getCommentFactory()}, {@code fetchUser()} (401 for guests), and
 * {@code getWdkModel()}.
 *
 * <p>SCAFFOLDING: all three endpoints return {@code 501 Not Implemented}.
 * Auth-gating and {@code @InSchema}/{@code @OutSchema} validation are wired in
 * deliverable 1 once the request/response schemas are fleshed out.
 */
@Path(AiGenePublicationCommentService.BASE_PATH)
public class AiGenePublicationCommentService extends AbstractUserCommentService {

  public static final String BASE_PATH    = "/user-comments/ai-gene-publication";
  public static final String JOB_ID_PARAM = "job-id";
  public static final String ID_PATH      = "/{" + JOB_ID_PARAM + "}";

  /**
   * Submit a job. Runs the sync prelude (validate, resolve synonyms, digest,
   * cache/registry lookup) and either returns a terminal cache-hit, attaches
   * the caller to an in-flight job, or spawns a fresh pipeline. Pool-full →
   * {@code 503} + {@code Retry-After}.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response submit(AiGenePublicationRequest body) throws WdkModelException {
    return notImplemented();
  }

  /** Poll the status of a job by its content-digest id. */
  @GET
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatus(@PathParam(JOB_ID_PARAM) String jobId) throws WdkModelException {
    return notImplemented();
  }

  /** Cancel an in-flight job. */
  @DELETE
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response cancel(@PathParam(JOB_ID_PARAM) String jobId) throws WdkModelException {
    return notImplemented();
  }

  private static Response notImplemented() {
    return Response.status(Response.Status.NOT_IMPLEMENTED)
        .type(MediaType.APPLICATION_JSON)
        .entity(new JSONObject()
            .put("type", "internal-error")
            .put("error", "not implemented yet (scaffolding)")
            .toString())
        .build();
  }
}
