package org.apidb.apicommon.model.comment.pojo;

import java.util.Date;

/**
 * Anonymous aggregate over {@code comment_ai_provenance} rows that point at the
 * same {@code run_job_id} — i.e. how many <em>other</em> users have already
 * published a comment from the same AI run. Provenance rows exist only for
 * published comments, so there is no "unreviewed" count.
 *
 * <p>Carried on the publishable terminal / cache-hit responses so the review
 * form can show "N others have published this combination" without revealing
 * any identities. {@code latestAt} is the most recent publish time (null when
 * no sibling exists yet).
 */
public class SiblingSummary {

  private final int _reviewed;   // published as-is (is_edited = false)
  private final int _edited;     // published with edits (is_edited = true)
  private final Date _latestAt;  // most recent publish, or null if none

  public SiblingSummary(int reviewed, int edited, Date latestAt) {
    _reviewed = reviewed;
    _edited = edited;
    _latestAt = latestAt;
  }

  public int getReviewed() { return _reviewed; }
  public int getEdited() { return _edited; }
  public Date getLatestAt() { return _latestAt; }
}
