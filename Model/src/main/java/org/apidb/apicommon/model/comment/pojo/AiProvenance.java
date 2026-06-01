package org.apidb.apicommon.model.comment.pojo;

import java.util.Date;

/**
 * POJO for a row in the {@code comment_ai_provenance} sidecar table — the
 * per-user review state for an AI-assisted comment, keyed by {@code comment_id}
 * with a FK to the shared {@link CommentAiRun} row via {@code run_job_id}.
 *
 * <p>When attached to a {@code CommentRequest}, signals that the comment being
 * created originated from an AI run and that a provenance row should be inserted
 * inside the same transaction (see deliverable 6).
 */
public class AiProvenance {

  /** {@code review_level} states the FE drives a comment through. */
  public enum ReviewLevel {
    UNREVIEWED("unreviewed"),
    REVIEWED("reviewed"),
    EDITED("edited");

    private final String _wire;

    ReviewLevel(String wire) { _wire = wire; }

    public String getWireValue() { return _wire; }

    public static ReviewLevel fromWire(String wire) {
      for (ReviewLevel level : values())
        if (level._wire.equals(wire))
          return level;
      throw new IllegalArgumentException("unknown review_level: " + wire);
    }
  }

  private long _commentId;
  private String _runJobId;
  private ReviewLevel _reviewLevel = ReviewLevel.UNREVIEWED;
  private Date _reviewedAt;             // null until review_level leaves UNREVIEWED

  public long getCommentId() { return _commentId; }
  public AiProvenance setCommentId(long commentId) { _commentId = commentId; return this; }

  public String getRunJobId() { return _runJobId; }
  public AiProvenance setRunJobId(String runJobId) { _runJobId = runJobId; return this; }

  public ReviewLevel getReviewLevel() { return _reviewLevel; }
  public AiProvenance setReviewLevel(ReviewLevel reviewLevel) { _reviewLevel = reviewLevel; return this; }

  public Date getReviewedAt() { return _reviewedAt; }
  public AiProvenance setReviewedAt(Date reviewedAt) { _reviewedAt = reviewedAt; return this; }
}
