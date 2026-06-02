package org.apidb.apicommon.service.services.ai;

import org.json.JSONObject;

/**
 * The terminal payload published on a {@link JobState} once the pipeline reaches
 * an end state. Rendered to the status-response JSON by the resource. Each
 * terminal {@link JobStatus} carries different fields; this carrier grows as the
 * pipeline deliverables land (success {@code ai_output}/{@code comment_id},
 * gene-not-mentioned {@code synonyms_checked}, validation-error {@code errors} …).
 *
 * <p>Deliverable 2 introduces the two outcomes the {@code fetching-article} stage
 * and the pipeline's top-level error handler can produce: {@code text-unavailable}
 * (carrying a {@code reason}) and {@code internal-error} (carrying an
 * {@code error} message).
 */
public final class TerminalResult {

  private final JobStatus _status;
  private final String _detail;   // text-unavailable: reason; internal-error: error message

  private TerminalResult(JobStatus status, String detail) {
    if (!status.isTerminal())
      throw new IllegalArgumentException("not a terminal status: " + status);
    _status = status;
    _detail = detail;
  }

  /** Article text could not be resolved (never persisted to the cache). */
  public static TerminalResult textUnavailable(String reason) {
    return new TerminalResult(JobStatus.TEXT_UNAVAILABLE, reason);
  }

  /** An unexpected pipeline failure (never persisted to the cache). */
  public static TerminalResult internalError(String error) {
    return new TerminalResult(JobStatus.INTERNAL_ERROR, error);
  }

  public JobStatus getStatus() { return _status; }

  /** The human-readable reason / error string, or null if none applies. */
  public String getDetail() { return _detail; }

  /** Render the wire JSON for a polling GET (and POST-time terminal responses). */
  public JSONObject toJson(String jobId) {
    JSONObject out = new JSONObject()
        .put("type", _status.getWireValue())
        .put("job_id", jobId);
    switch (_status) {
      case TEXT_UNAVAILABLE:
        out.put("reason", _detail);
        break;
      case INTERNAL_ERROR:
        out.put("error", _detail);
        break;
      default:
        // success / gene-not-mentioned / mentioned-in-passing payloads land in
        // later deliverables.
        break;
    }
    return out;
  }
}
