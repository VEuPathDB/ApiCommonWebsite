package org.apidb.apicommon.service.services.ai;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;

import org.apidb.apicommon.model.comment.CommentFactory;
import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.apidb.apicommon.service.services.ai.gene.GeneSynonymService;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

/**
 * Stage 0 — the synchronous prelude that runs entirely on the request thread
 * (target &lt;2 s, no LLM calls). Validates the request, resolves gene synonyms
 * in-process, computes the content-digest {@code jobId}, then consults the
 * {@code comment_ai_run} cache and the in-memory {@link JobRegistry} to either
 * return a terminal cache-hit, attach the caller as a follower of an in-flight
 * job, or spawn a fresh pipeline.
 */
public class SyncPrelude {

  private final WdkModel _wdkModel;
  private final JobRegistry _registry;
  private final CommentFactory _commentFactory;
  private final GeneSynonymService _geneSynonyms;

  public SyncPrelude(WdkModel wdkModel, JobRegistry registry, CommentFactory commentFactory) {
    _wdkModel = wdkModel;
    _registry = registry;
    _commentFactory = commentFactory;
    _geneSynonyms = new GeneSynonymService(wdkModel);
  }

  private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

  /**
   * 0a — validate request shape. PubMed requests need a {@code pubmed_id};
   * upload requests need non-empty {@code paper_text} and a 64-char hex
   * {@code pdf_content_sha256}. Throws {@link BadRequestException} (400) on any
   * violation. Static + dependency-free so it is unit-testable in isolation.
   */
  public static void validate(AiGenePublicationRequest request) {
    if (request == null)
      throw new BadRequestException("request body is required");
    if (isBlank(request.geneId))
      throw new BadRequestException("gene_id is required");

    String type = request.documentType == null ? "" : request.documentType.trim();
    switch (type) {
      case "pubmed":
        if (isBlank(request.pubmedId))
          throw new BadRequestException("pubmed_id is required when document_type=pubmed");
        // external_ref is an upload-only field; never carry it on the pubmed path.
        request.externalRef = null;
        request.externalRefKind = null;
        break;
      case "upload":
        if (isBlank(request.paperText))
          throw new BadRequestException("paper_text is required when document_type=upload");
        if (request.pdfContentSha256 == null
            || !SHA256_HEX.matcher(request.pdfContentSha256).matches())
          throw new BadRequestException(
              "pdf_content_sha256 must be a 64-character hex string when document_type=upload");
        ExternalRef.Result ref = ExternalRef.normalise(request.externalRef, request.externalRefKind);
        request.externalRef = ref.ref;
        request.externalRefKind = ref.kind;
        break;
      default:
        throw new BadRequestException("document_type must be 'pubmed' or 'upload'");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  /** 0b — resolve the gene's aliases (throws 404 if the stable id is unknown). */
  public List<String> resolveSynonyms(String geneId) throws WdkModelException {
    return _geneSynonyms.resolve(geneId);
  }

  /**
   * 0c — compute {@code job_id = sha256(geneId | sortedSynonyms | sourceKey |
   * modelName | promptVersion | optionsCanonicalJson)}. The source key is the
   * PubMed id (pubmed path) or the FE-supplied PDF content hash (upload path).
   */
  public String computeJobId(AiGenePublicationRequest request, List<String> synonyms) {
    return JobDigest.compute(
        request.geneId,
        synonyms,
        sourceKey(request),
        AiSummaryConfig.MODEL_NAME,
        AiSummaryConfig.PROMPT_VERSION,
        JobDigest.canonicalOptionsJson(request.options));
  }

  private static String sourceKey(AiGenePublicationRequest request) {
    return "upload".equals(request.documentType)
        ? request.pdfContentSha256
        : request.pubmedId;
  }

  /**
   * Run the whole prelude (0a–0f) for a submitter and report the outcome the
   * resource should translate into a response. Throws
   * {@link java.util.concurrent.RejectedExecutionException} when the pool is
   * saturated (→ 503), {@link BadRequestException} on invalid input (→ 400),
   * and {@link javax.ws.rs.NotFoundException} for an unknown gene (→ 404).
   */
  public PreludeResult handleSubmit(AiGenePublicationRequest request, long userId)
      throws WdkModelException {
    validate(request);                                          // 0a
    List<String> synonyms = resolveSynonyms(request.geneId);    // 0b
    String jobId = computeJobId(request, synonyms);             // 0c

    Optional<CommentAiRun> cached = _commentFactory.findAiRun(jobId);  // 0d
    if (cached.isPresent()) {
      return PreludeResult.cacheHit(jobId, cached.get());
    }

    if (_registry.get(jobId).isPresent()) {                     // 0e
      JobState attached = _registry.attach(jobId, userId);
      if (attached != null) {
        return PreludeResult.running(attached);
      }
      // raced with eviction between get() and attach(): fall through to spawn
    }

    JobSubmission submission = new JobSubmission(request, jobId, synonyms,  // 0f
        AiSummaryConfig.MODEL_NAME, AiSummaryConfig.PROMPT_VERSION,
        JobDigest.canonicalOptionsJson(request.options));
    JobState job = _registry.submit(submission, userId,
        jobState -> new AiGenePublicationPipeline(jobState, _wdkModel));
    return PreludeResult.running(job);
  }

  /** Outcome of the prelude: either a cache hit on {@code comment_ai_run} or a live (running) job. */
  public static final class PreludeResult {

    public enum Kind { CACHE_HIT, RUNNING }

    private final Kind _kind;
    private final String _jobId;
    private final CommentAiRun _cacheRow;  // non-null iff CACHE_HIT
    private final JobState _jobState;      // non-null iff RUNNING

    private PreludeResult(Kind kind, String jobId, CommentAiRun cacheRow, JobState jobState) {
      _kind = kind;
      _jobId = jobId;
      _cacheRow = cacheRow;
      _jobState = jobState;
    }

    static PreludeResult cacheHit(String jobId, CommentAiRun cacheRow) {
      return new PreludeResult(Kind.CACHE_HIT, jobId, cacheRow, null);
    }

    static PreludeResult running(JobState jobState) {
      return new PreludeResult(Kind.RUNNING, jobState.getJobId(), null, jobState);
    }

    public Kind getKind() { return _kind; }
    public String getJobId() { return _jobId; }
    public CommentAiRun getCacheRow() { return _cacheRow; }
    public JobState getJobState() { return _jobState; }
  }
}
