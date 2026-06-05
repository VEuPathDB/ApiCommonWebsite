package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
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
  private final List<String> _synonymsChecked;  // gene-not-mentioned: gene id + aliases searched

  private TerminalResult(JobStatus status, String detail, List<String> synonymsChecked) {
    if (!status.isTerminal())
      throw new IllegalArgumentException("not a terminal status: " + status);
    _status = status;
    _detail = detail;
    _synonymsChecked = synonymsChecked;
  }

  /** Article text could not be resolved (never persisted to the cache). */
  public static TerminalResult textUnavailable(String reason) {
    return new TerminalResult(JobStatus.TEXT_UNAVAILABLE, reason, null);
  }

  /** An unexpected pipeline failure (never persisted to the cache). */
  public static TerminalResult internalError(String error) {
    return new TerminalResult(JobStatus.INTERNAL_ERROR, error, null);
  }

  /**
   * The deterministic gene-mention scan found neither the gene id nor any alias.
   * {@code synonymsChecked} records everything searched for (gene id first); this
   * outcome <em>is</em> persisted to {@code comment_ai_run}.
   */
  public static TerminalResult geneNotMentioned(List<String> synonymsChecked) {
    return new TerminalResult(JobStatus.GENE_NOT_MENTIONED, null,
        Collections.unmodifiableList(new ArrayList<>(synonymsChecked)));
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
      case GENE_NOT_MENTIONED:
        out.put("synonyms_checked", new JSONArray(_synonymsChecked));
        // sibling_summary aggregate is added once the DB tables land (D6/D7).
        break;
      default:
        // success / mentioned-in-passing payloads land in later deliverables.
        break;
    }
    return out;
  }
}
