package org.apidb.apicommon.service.services.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apidb.apicommon.model.comment.pojo.JobStatus;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The terminal payload published on a {@link JobState} once the pipeline reaches
 * an end state. Rendered to the status-response JSON by the resource. Each
 * terminal {@link JobStatus} carries different fields; this carrier grows as the
 * pipeline deliverables land (success {@code ai_output},
 * gene-not-mentioned / mentioned-in-passing {@code synonyms_checked} …).
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
  private final String _headline;  // success: ai_output headline
  private final String _content;   // success: ai_output content

  private TerminalResult(JobStatus status, String detail, List<String> synonymsChecked,
      String headline, String content) {
    if (!status.isTerminal())
      throw new IllegalArgumentException("not a terminal status: " + status);
    _status = status;
    _detail = detail;
    _synonymsChecked = synonymsChecked;
    _headline = headline;
    _content = content;
  }

  /**
   * The pipeline produced a publishable summary (persisted to {@code comment_ai_run}).
   * Carries the flattened {@code ai_output} the front end renders for review — the
   * same shape a late cache hit returns from the run row.
   */
  public static TerminalResult success(String headline, String content) {
    return new TerminalResult(JobStatus.SUCCESS, null, null, headline, content);
  }

  /** Article text could not be resolved (never persisted to the cache). */
  public static TerminalResult textUnavailable(String reason) {
    return new TerminalResult(JobStatus.TEXT_UNAVAILABLE, reason, null, null, null);
  }

  /** An unexpected pipeline failure (never persisted to the cache). */
  public static TerminalResult internalError(String error) {
    return new TerminalResult(JobStatus.INTERNAL_ERROR, error, null, null, null);
  }

  /**
   * The deterministic gene-mention scan found neither the gene id nor any alias.
   * {@code synonymsChecked} records everything searched for (gene id first); this
   * outcome <em>is</em> persisted to {@code comment_ai_run}.
   */
  public static TerminalResult geneNotMentioned(List<String> synonymsChecked) {
    return new TerminalResult(JobStatus.GENE_NOT_MENTIONED, null,
        Collections.unmodifiableList(new ArrayList<>(synonymsChecked)), null, null);
  }

  /**
   * The LLM judged the gene only mentioned in passing ({@code only_in_passing=true}).
   * Carries {@code synonyms_checked} like {@code gene-not-mentioned} and is likewise
   * persisted to {@code comment_ai_run}.
   */
  public static TerminalResult mentionedInPassing(List<String> synonymsChecked) {
    return new TerminalResult(JobStatus.MENTIONED_IN_PASSING, null,
        Collections.unmodifiableList(new ArrayList<>(synonymsChecked)), null, null);
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
      case SUCCESS:
        out.put("ai_output", new JSONObject()
            .put("headline", _headline)
            .put("content", _content));
        break;
      case TEXT_UNAVAILABLE:
        out.put("reason", _detail);
        break;
      case INTERNAL_ERROR:
        out.put("error", _detail);
        break;
      case GENE_NOT_MENTIONED:
      case MENTIONED_IN_PASSING:
        out.put("synonyms_checked", new JSONArray(_synonymsChecked));
        // the `source` object is attached at the service layer (it reads the
        // submission / cache row, which this pure carrier doesn't hold).
        break;
      default:
        break;
    }
    return out;
  }
}
