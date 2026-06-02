package org.apidb.apicommon.service.services.ai;

import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.service.services.ai.SyncPrelude.PreludeResult;
import org.apidb.apicommon.service.services.comments.AbstractUserCommentService;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.json.JSONObject;

/**
 * AI-assisted gene-publication summary service. A user supplies a gene
 * ({@code gene_id}) and a publication (a PubMed id, or paper text the front-end
 * extracted from an uploaded PDF). The service resolves the publication text,
 * verifies the gene is mentioned, runs an LLM to summarise the gene's function,
 * and persists a {@code user_comment} carrying AI provenance.
 *
 * <p>Three endpoints, matching the front-end contract
 * ({@code CLAUDE-ai-user-comments.md}). JSON-only end to end. The POST runs the
 * synchronous prelude then spawns an async pipeline; GET polls job status;
 * DELETE cancels.
 *
 * <p>NOTE (deliverable 1): the prelude (validate → resolve synonyms → digest →
 * cache/registry/spawn) is wired here. The terminal cache-hit response does not
 * yet create the submitter's own comment or aggregate {@code sibling_summary}
 * (deliverable 6), and the async pipeline body (stages ①–⑥) lands in
 * deliverables 2–6, so a freshly-spawned job stays in {@code running}.
 */
@Path(AiGenePublicationCommentService.BASE_PATH)
public class AiGenePublicationCommentService extends AbstractUserCommentService {

  public static final String BASE_PATH    = "/user-comments/ai-gene-publication";
  public static final String JOB_ID_PARAM = "job-id";
  public static final String ID_PATH      = "/{" + JOB_ID_PARAM + "}";

  /** Seconds advertised in {@code Retry-After} when the pipeline pool is saturated. */
  private static final int RETRY_AFTER_SECONDS = 30;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response submit(AiGenePublicationRequest body) throws WdkModelException {
    User user = fetchUser(); // 401 for guests
    SyncPrelude prelude = new SyncPrelude(getWdkModel(), JobRegistry.instance(), getCommentFactory());
    try {
      PreludeResult result = prelude.handleSubmit(body, user.getUserId());
      return Response.ok(preludeJson(result).toString()).build();
    }
    catch (RejectedExecutionException poolFull) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
          .header("Retry-After", RETRY_AFTER_SECONDS)
          .type(MediaType.APPLICATION_JSON)
          .entity(new JSONObject()
              .put("type", "internal-error")
              .put("error", "summary workers are busy; please retry shortly")
              .toString())
          .build();
    }
  }

  @GET
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatus(@PathParam(JOB_ID_PARAM) String jobId) throws WdkModelException {
    fetchUser(); // require login

    Optional<JobState> live = JobRegistry.instance().get(jobId);
    if (live.isPresent()) {
      return Response.ok(jobStateJson(live.get()).toString()).build();
    }

    // Registry entry evicted (or never existed in this JVM): a permanent
    // comment_ai_run row still satisfies a late cache hit.
    Optional<CommentAiRun> cached = getCommentFactory().findAiRun(jobId);
    if (cached.isPresent()) {
      return Response.ok(cacheHitJson(jobId, cached.get()).toString()).build();
    }

    // Unknown / expired job → 404; the FE shows "job expired, please resubmit".
    throw new NotFoundException("No AI gene-publication job for id '" + jobId + "'");
  }

  @DELETE
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response cancel(@PathParam(JOB_ID_PARAM) String jobId) throws WdkModelException {
    // Cancellation wiring lands in deliverable 7.
    return Response.status(Response.Status.NOT_IMPLEMENTED)
        .type(MediaType.APPLICATION_JSON)
        .entity(new JSONObject().put("type", "internal-error")
            .put("error", "cancellation not implemented yet").toString())
        .build();
  }

  // --- response shaping -------------------------------------------------------

  private static JSONObject preludeJson(PreludeResult result) {
    switch (result.getKind()) {
      case CACHE_HIT:
        return cacheHitJson(result.getJobId(), result.getCacheRow());
      case RUNNING:
      default:
        return jobStateJson(result.getJobState());
    }
  }

  private static JSONObject jobStateJson(JobState job) {
    if (job.getStatus() == JobStatus.RUNNING) {
      return new JSONObject()
          .put("type", JobStatus.RUNNING.getWireValue())
          .put("job_id", job.getJobId())
          .put("stage", job.getStage().getWireValue());
    }
    // Terminal payloads (ai_output, comment_id, sibling_summary, reason, errors)
    // are populated once the pipeline + persist stages land (deliverables 2-6).
    return new JSONObject()
        .put("type", job.getStatus().getWireValue())
        .put("job_id", job.getJobId());
  }

  private static JSONObject cacheHitJson(String jobId, CommentAiRun run) {
    // terminal_status is one of: success | mentioned-in-passing | gene-not-mentioned.
    JSONObject out = new JSONObject()
        .put("type", run.getTerminalStatus())
        .put("job_id", jobId);
    if ("success".equals(run.getTerminalStatus())) {
      out.put("ai_output", new JSONObject()
          .put("headline", run.getAiHeadline())
          .put("content", run.getAiContent()));
    }
    // TODO(deliverable 6): create the submitter's own comment inline and add
    // comment_id, plus the anonymous sibling_summary aggregate.
    return out;
  }
}
