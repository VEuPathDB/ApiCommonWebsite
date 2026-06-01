package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.VARCHAR;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;

/**
 * Look up a single {@code comment_ai_run} row by its content-digest
 * {@code job_id}. A hit short-circuits a fresh submit straight to a terminal
 * cache-hit response (sync prelude step 0d).
 *
 * <p>Follows the {@link ValueQuery} read pattern ({@code parseResults} /
 * {@code getParams} / {@code getTypes}).
 *
 * <p>SCAFFOLDING: SELECT shape is final; row mapping is wired in deliverable 1.
 * The anonymous {@code sibling_summary} aggregate over
 * {@code comment_ai_provenance} is a separate query, added alongside.
 */
public class GetCommentAiRunQuery extends ValueQuery<Optional<CommentAiRun>> {

  private static final String SQL = "SELECT" +
      " job_id, model_name, prompt_version, source_kind, pubmed_id," +
      " external_url, external_title, pdf_content_sha256, gene_id, synonyms_used," +
      " options_json, terminal_status, is_only_mentioned_in_passing," +
      " ai_headline, ai_content, completed_at" +
      " FROM %s." + Table.COMMENT_AI_RUN +
      " WHERE job_id = ?";

  private static final Integer[] TYPES = { VARCHAR };

  private final String _jobId;

  public GetCommentAiRunQuery(String schema, String jobId) {
    super(schema);
    _jobId = jobId;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }

  @Override
  protected Optional<CommentAiRun> parseResults(ResultSet rs) throws SQLException {
    throw new UnsupportedOperationException(
        "GetCommentAiRunQuery row mapping — deliverable 1");
  }

  @Override
  protected Object[] getParams() {
    return new Object[] { _jobId };
  }

  @Override
  protected Integer[] getTypes() {
    return TYPES;
  }
}
