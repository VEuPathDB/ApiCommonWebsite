package org.apidb.apicommon.service.services.ai;

/**
 * The {@code type} discriminator on a status response, matching the front-end
 * contract union (see {@code CLAUDE-ai-user-comments.md}). {@link #RUNNING} is
 * the only non-terminal value; while running, the {@link JobState.Stage} carries
 * the finer-grained progress.
 */
public enum JobStatus {

  RUNNING("running", false),

  // terminal — persisted to comment_ai_run (future submits cache-hit):
  SUCCESS("success", true),
  MENTIONED_IN_PASSING("mentioned-in-passing", true),
  GENE_NOT_MENTIONED("gene-not-mentioned", true),

  // terminal — NOT persisted (retries are free):
  TEXT_UNAVAILABLE("text-unavailable", true),
  INTERNAL_ERROR("internal-error", true),
  CANCELLED("cancelled", true);

  private final String _wire;
  private final boolean _terminal;

  JobStatus(String wire, boolean terminal) {
    _wire = wire;
    _terminal = terminal;
  }

  /** The snake/kebab-case value sent on the wire. */
  public String getWireValue() { return _wire; }

  public boolean isTerminal() { return _terminal; }

  /**
   * The three terminal outcomes that are persisted to {@code comment_ai_run} and
   * from which a user may publish a comment. These are exactly the terminals
   * whose responses carry a {@code source} object.
   */
  public boolean isPublishable() {
    return this == SUCCESS || this == MENTIONED_IN_PASSING || this == GENE_NOT_MENTIONED;
  }
}
