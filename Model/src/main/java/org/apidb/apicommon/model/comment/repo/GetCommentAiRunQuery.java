package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.VARCHAR;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;

/**
 * Look up a single {@code comment_ai_run} row by its content-digest
 * {@code job_id}. A hit short-circuits a fresh submit straight to a terminal
 * cache-hit response (sync prelude step 0d).
 *
 * <p>Follows the {@link ValueQuery} read pattern ({@code parseResults} /
 * {@code getParams} / {@code getTypes}).
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
    if (!rs.next())
      return Optional.empty();

    CommentAiRun run = new CommentAiRun()
        .setJobId(rs.getString("job_id"))
        .setModelName(rs.getString("model_name"))
        .setPromptVersion(rs.getString("prompt_version"))
        .setSourceKind(rs.getString("source_kind"))
        .setPubmedId(rs.getString("pubmed_id"))
        .setExternalUrl(rs.getString("external_url"))
        .setExternalTitle(rs.getString("external_title"))
        .setPdfContentSha256(rs.getString("pdf_content_sha256"))
        .setGeneId(rs.getString("gene_id"))
        .setSynonymsUsed(toStringList(rs.getArray("synonyms_used")))
        .setOptionsJson(rs.getString("options_json"))
        .setTerminalStatus(rs.getString("terminal_status"))
        .setOnlyMentionedInPassing(rs.getBoolean("is_only_mentioned_in_passing"))
        .setAiHeadline(rs.getString("ai_headline"))
        .setAiContent(rs.getString("ai_content"))
        .setCompletedAt(rs.getTimestamp("completed_at"));

    return Optional.of(run);
  }

  private static List<String> toStringList(Array sqlArray) throws SQLException {
    if (sqlArray == null)
      return Collections.emptyList();
    Object raw = sqlArray.getArray();
    if (!(raw instanceof Object[]))
      return Collections.emptyList();
    Object[] elements = (Object[]) raw;
    List<String> out = new ArrayList<>(elements.length);
    for (Object e : elements)
      if (e != null) out.add(e.toString());
    return out;
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
