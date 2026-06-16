package org.apidb.apicommon.service.services.ai;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import javax.ws.rs.BadRequestException;
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

import org.apidb.apicommon.model.comment.pojo.AiProvenance;
import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.model.comment.pojo.CommentRequest;
import org.apidb.apicommon.model.comment.pojo.SiblingSummary;
import org.apidb.apicommon.model.comment.pojo.Target;
import org.apidb.apicommon.service.services.ai.SyncPrelude.PreludeResult;
import org.apidb.apicommon.service.services.ai.gene.GeneSynonymService;
import org.apidb.apicommon.service.services.comments.AbstractUserCommentService;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.User;
import org.json.JSONArray;
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

  /**
   * Cancel an in-flight job. v1 cancels the underlying run for <em>all</em>
   * attached followers (no per-follower detach). Idempotent: cancelling an
   * unknown / already-evicted / already-terminal job is a no-op. Returns 204;
   * the FE's next poll observes the {@code cancelled} terminal (or
   * {@code not-found} once evicted).
   */
  @DELETE
  @Path(ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response cancel(@PathParam(JOB_ID_PARAM) String jobId) throws WdkModelException {
    fetchUser(); // 401 for guests
    JobRegistry.instance().cancel(jobId);
    return Response.noContent().build();
  }

  /**
   * Create the user comment on approval. Body carries only the (possibly edited)
   * reviewed {@code headline} / {@code content}; the gene target and AI
   * provenance come from the cached {@code comment_ai_run} row keyed by the
   * path's {@code job_id}, and {@code is_edited} is derived server-side. This is
   * the only call that creates a {@code comments} row in the AI flow.
   *
   * <p>404 if the {@code job_id} has no cached run (the non-publishable
   * {@code text-unavailable} / {@code internal-error} outcomes are never
   * persisted); 400 if {@code headline} / {@code content} are blank; 201 with
   * {@code { comment_id }} on success (comment + provenance written in one tx).
   */
  @POST
  @Path(ID_PATH + "/publish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response publish(@PathParam(JOB_ID_PARAM) String jobId, PublishRequest body)
      throws WdkModelException {
    User user = fetchUser(); // 401 for guests

    String headline = body == null ? null : body.headline;
    String content  = body == null ? null : body.content;
    if (isBlank(headline) || isBlank(content))
      throw new BadRequestException("headline and content are required");

    // Only persisted (publishable) terminals have a run row; an absent row is a
    // clean 404 (e.g. a text-unavailable / internal-error outcome).
    CommentAiRun run = getCommentFactory().findAiRun(jobId)
        .orElseThrow(() -> new NotFoundException(
            "No publishable AI gene-publication run for id '" + jobId + "'"));

    CommentRequest request = buildPublishComment(run, headline, content, new Date());
    long commentId = getCommentFactory().createComment(request, user);

    return Response.status(Response.Status.CREATED)
        .type(MediaType.APPLICATION_JSON)
        .entity(new JSONObject().put("comment_id", commentId).toString())
        .build();
  }

  /**
   * Build the comment to create on publish from the cached run row plus the
   * user-submitted (possibly edited) text. The comment targets the gene; AI
   * provenance carries the run id and {@code is_edited} — true whenever the
   * submitted text differs from the run's AI original, which includes the case
   * where there was no AI original at all ({@code gene-not-mentioned} /
   * {@code mentioned-in-passing}, whose run rows carry null ai_headline/ai_content).
   *
   * <p>TODO(post-impl): resolve and set the comment ORGANISM from the gene record
   * (GeneRecordClasses.GeneRecordClass) rather than leaving it null. Organism is
   * optional for comment creation, so v1 omits it; see the plan's follow-ups.
   */
  static CommentRequest buildPublishComment(CommentAiRun run, String headline,
      String content, Date now) {
    boolean edited = !Objects.equals(headline, run.getAiHeadline())
                  || !Objects.equals(content, run.getAiContent());

    AiProvenance provenance = new AiProvenance()
        .setRunJobId(run.getJobId())
        .setEdited(edited)
        .setCreatedAt(now);

    CommentRequest request = new CommentRequest();
    request.setHeadline(headline);
    request.setContent(content);
    request.setTarget(new Target()
        .setType(GeneSynonymService.GENE_URL_SEGMENT)
        .setId(run.getGeneId()));
    request.setAiProvenance(provenance);
    return request;
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  // --- response shaping -------------------------------------------------------

  private JSONObject preludeJson(PreludeResult result) throws WdkModelException {
    switch (result.getKind()) {
      case CACHE_HIT:
        return cacheHitJson(result.getJobId(), result.getCacheRow());
      case RUNNING:
      default:
        return jobStateJson(result.getJobState());
    }
  }

  private JSONObject jobStateJson(JobState job) throws WdkModelException {
    if (job.getStatus() == JobStatus.RUNNING) {
      return new JSONObject()
          .put("type", JobStatus.RUNNING.getWireValue())
          .put("job_id", job.getJobId())
          .put("stage", job.getStage().getWireValue());
    }
    // Terminal: the pipeline publishes a TerminalResult carrying the status-
    // specific fields (text-unavailable `reason`, internal-error `error`,
    // success `ai_output`, gene-not-mentioned / mentioned-in-passing
    // `synonyms_checked`). The publishable terminals additionally carry the
    // anonymous sibling_summary aggregate, attached here at the service layer
    // (it needs a DB read, so it can't live on the pure TerminalResult).
    if (job.getResult() instanceof TerminalResult) {
      JSONObject out = ((TerminalResult) job.getResult()).toJson(job.getJobId());
      if (job.getStatus().isPublishable())
        out.put("sibling_summary",
            siblingSummaryJson(getCommentFactory().getSiblingSummary(job.getJobId())));
      return out;
    }
    return new JSONObject()
        .put("type", job.getStatus().getWireValue())
        .put("job_id", job.getJobId());
  }

  private JSONObject cacheHitJson(String jobId, CommentAiRun run) throws WdkModelException {
    // terminal_status is one of: success | mentioned-in-passing | gene-not-mentioned
    // (only publishable terminals are persisted), so a cache hit always carries
    // a sibling_summary. No comment_id — the comment is created only on publish.
    JSONObject out = new JSONObject()
        .put("type", run.getTerminalStatus())
        .put("job_id", jobId);
    if ("success".equals(run.getTerminalStatus())) {
      out.put("ai_output", new JSONObject()
          .put("headline", run.getAiHeadline())
          .put("content", run.getAiContent()));
    } else {
      out.put("synonyms_checked", new JSONArray(run.getSynonymsUsed()));
    }
    out.put("sibling_summary", siblingSummaryJson(getCommentFactory().getSiblingSummary(jobId)));
    return out;
  }

  /**
   * Render the anonymous {@link SiblingSummary} to its wire shape
   * {@code { reviewed, edited, latest_at }}; {@code latest_at} is ISO-8601 UTC or
   * {@code null}. Pure — the counts are fetched by the caller.
   */
  static JSONObject siblingSummaryJson(SiblingSummary summary) {
    return new JSONObject()
        .put("reviewed", summary.getReviewed())
        .put("edited", summary.getEdited())
        .put("latest_at", summary.getLatestAt() == null
            ? JSONObject.NULL
            : summary.getLatestAt().toInstant().toString());
  }
}
