package org.apidb.apicommon.service.services.ai;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * Mutable in-memory state for one AI gene-publication job, keyed in
 * {@link JobRegistry} by the content-digest {@code jobId} (not a UUID).
 *
 * <p>Three tiers of state:
 * <ul>
 *   <li><b>Shared, immutable inputs</b> — the {@link JobSubmission} (gene,
 *       synonyms, source, model, options, …), identical across all followers.</li>
 *   <li><b>Per-follower identity</b> — the list of follower user ids (used for
 *       dedupe/observability only; followers no longer receive a per-user
 *       comment, since comments are created by the publish endpoint on user
 *       approval rather than by the pipeline).</li>
 *   <li><b>Published progress / terminal</b> — the volatile {@code stage} that
 *       polling GETs observe and, once terminal, the {@code result}. Transient
 *       per-stage outputs (article text, scan counts, summary JSON) live on the
 *       pipeline instance, not here.</li>
 * </ul>
 *
 * <p>Server restart loses all {@code JobState}s; the durable artefacts are the
 * {@code comment_ai_run} / {@code comments} / {@code comment_ai_provenance} rows.
 */
public class JobState {

  /** Running-stage names emitted in the {@code running} response's {@code stage} field. */
  public enum Stage {
    QUEUED("queued"),
    FETCHING_ARTICLE("fetching-article"),
    SCANNING_GENE_MENTIONS("scanning-gene-mentions"),
    GENERATING_SUMMARY("generating-summary"),
    VALIDATING("validating"),
    PERSISTING("persisting");

    private final String _wire;

    Stage(String wire) { _wire = wire; }

    public String getWireValue() { return _wire; }
  }

  private final JobSubmission _submission;
  private volatile JobStatus _status = JobStatus.RUNNING;
  private volatile Stage _stage = Stage.QUEUED;
  private volatile String _message;
  private volatile Date _updatedAt = new Date();
  private volatile Date _terminalAt;          // when status became terminal (drives TTL eviction)
  private volatile Future<?> _future;
  private final List<Long> _followerUserIds = new CopyOnWriteArrayList<>();
  private volatile Object _result;            // terminal payload (TerminalResult)

  public JobState(JobSubmission submission, long firstFollowerUserId) {
    _submission = submission;
    _followerUserIds.add(firstFollowerUserId);
  }

  public JobSubmission getSubmission() { return _submission; }
  public String getJobId() { return _submission.getJobId(); }

  public JobStatus getStatus() { return _status; }
  public Stage getStage() { return _stage; }
  public String getMessage() { return _message; }
  public Date getUpdatedAt() { return _updatedAt; }
  public Date getTerminalAt() { return _terminalAt; }
  public Future<?> getFuture() { return _future; }
  public void setFuture(Future<?> future) { _future = future; }
  public Object getResult() { return _result; }

  public List<Long> getFollowerUserIds() { return _followerUserIds; }

  /** Attach another caller to this in-flight job (idempotent per user id). */
  public void addFollower(long userId) {
    if (!_followerUserIds.contains(userId))
      _followerUserIds.add(userId);
  }

  /** Advance the running stage and stamp the update time. */
  public void updateStage(Stage stage, String message) {
    _stage = stage;
    _message = message;
    _updatedAt = new Date();
  }

  /** Move the job to a terminal status, recording the result and terminal time. */
  public void markTerminal(JobStatus status, Object result) {
    if (!status.isTerminal())
      throw new IllegalArgumentException("not a terminal status: " + status);
    _status = status;
    _result = result;
    _updatedAt = new Date();
    _terminalAt = _updatedAt;
  }
}
