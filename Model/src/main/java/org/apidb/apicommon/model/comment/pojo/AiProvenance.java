package org.apidb.apicommon.model.comment.pojo;

import java.util.Date;
import java.util.Objects;

/**
 * POJO for a row in the {@code comment_ai_provenance} sidecar table, keyed by
 * {@code comment_id} with a FK to the shared {@link CommentAiRun} row via
 * {@code run_job_id}.
 *
 * <p>A provenance row exists <b>only for a published comment</b> (created by the
 * publish endpoint on user approval, never by the pipeline), so there is no
 * multi-valued review level — every such row is already reviewed-and-approved.
 * The single {@code is_edited} flag records whether the user changed the AI's
 * original text before publishing, and {@code created_at} is the approval time.
 *
 * <p>When attached to a {@code CommentRequest}, signals that the comment being
 * created originated from an AI run and that a provenance row should be inserted
 * inside the same transaction.
 */
public class AiProvenance {

  private long _commentId;
  private String _runJobId;
  private boolean _edited;              // true iff the published text differs from the AI original
  private Date _createdAt;              // when the user approved/published

  public long getCommentId() { return _commentId; }
  public AiProvenance setCommentId(long commentId) { _commentId = commentId; return this; }

  public String getRunJobId() { return _runJobId; }
  public AiProvenance setRunJobId(String runJobId) { _runJobId = runJobId; return this; }

  public boolean isEdited() { return _edited; }
  public AiProvenance setEdited(boolean edited) { _edited = edited; return this; }

  public Date getCreatedAt() { return _createdAt; }
  public AiProvenance setCreatedAt(Date createdAt) { _createdAt = createdAt; return this; }

  /**
   * Build the per-comment provenance row for a comment being created from an AI
   * run: carries the run's {@code job_id} as the FK and derives {@code is_edited}
   * by comparing the submitted {@code headline}/{@code content} to the run's AI
   * original. Shared by the publish endpoint (initial approval) and the
   * carry-forward when a published AI comment is edited. {@code commentId} is set
   * later by {@code createComment} once the new id is known.
   *
   * @param now the approval/edit time recorded as {@code created_at}
   */
  public static AiProvenance fromRun(CommentAiRun run, String headline, String content, Date now) {
    boolean edited = !Objects.equals(headline, run.getAiHeadline())
                  || !Objects.equals(content, run.getAiContent());
    return new AiProvenance()
        .setRunJobId(run.getJobId())
        .setEdited(edited)
        .setCreatedAt(now);
  }
}
