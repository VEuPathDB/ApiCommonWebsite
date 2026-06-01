package org.apidb.apicommon.service.services.ai;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * Mutable in-memory state for one AI gene-publication job, keyed in
 * {@link JobRegistry} by the content-digest {@code jobId} (not a UUID). Carries
 * the current progress, the running {@link Future}, the follower list (callers
 * who attached to the same digest and each get their own comment at persist
 * time), and — once terminal — the result payload.
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

  /**
   * A caller attached to this job. Each follower receives its own
   * {@code comment_id} when the persist stage loops over the follower list.
   */
  public static final class Submitter {
    private final long _userId;
    private final AiGenePublicationRequest _request;
    private volatile Long _commentId;   // set after the persist stage creates the comment

    public Submitter(long userId, AiGenePublicationRequest request) {
      _userId = userId;
      _request = request;
    }

    public long getUserId() { return _userId; }
    public AiGenePublicationRequest getRequest() { return _request; }
    public Long getCommentId() { return _commentId; }
    public void setCommentId(Long commentId) { _commentId = commentId; }
  }

  private final String _jobId;
  private volatile JobStatus _status = JobStatus.RUNNING;
  private volatile Stage _stage = Stage.QUEUED;
  private volatile String _message;
  private volatile Date _updatedAt = new Date();
  private volatile Date _terminalAt;          // when status became terminal (drives TTL eviction)
  private volatile Future<?> _future;
  private final List<Submitter> _followers = new CopyOnWriteArrayList<>();
  private volatile Object _result;            // terminal payload; concrete type added with the pipeline

  public JobState(String jobId, Submitter initialSubmitter) {
    _jobId = jobId;
    _followers.add(initialSubmitter);
  }

  public String getJobId() { return _jobId; }

  public JobStatus getStatus() { return _status; }
  public Stage getStage() { return _stage; }
  public String getMessage() { return _message; }
  public Date getUpdatedAt() { return _updatedAt; }
  public Date getTerminalAt() { return _terminalAt; }
  public Future<?> getFuture() { return _future; }
  public void setFuture(Future<?> future) { _future = future; }
  public Object getResult() { return _result; }

  public List<Submitter> getFollowers() { return _followers; }
  public void addFollower(Submitter submitter) { _followers.add(submitter); }

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
