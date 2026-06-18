package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.BIGINT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apidb.apicommon.model.comment.pojo.AiProvenanceView;

/**
 * Read the AI provenance for a set of published comments, joining the
 * per-comment {@code comment_ai_provenance} row ({@code is_edited} + FK) to its
 * shared {@code comment_ai_run} cache row (article source + original AI
 * headline/content) via {@code run_job_id}.
 *
 * <p>Returns a {@code Map<commentId, AiProvenanceView>} containing only the
 * comments that actually have provenance; human-written comments are simply
 * absent from the map. The {@code comment_id IN (…)} placeholders are expanded
 * to match the id count before the schema is interpolated, so callers must pass
 * a non-empty id collection.
 */
public class GetCommentAiProvenanceQuery extends ValueQuery<Map<Long, AiProvenanceView>> {

  private final Long[] _commentIds;

  public GetCommentAiProvenanceQuery(String schema, Collection<Long> commentIds) {
    super(schema);
    _commentIds = commentIds.toArray(new Long[0]);
  }

  @Override
  protected String getQuery() {
    // Build one bind placeholder per id; the schema is filled later by
    // Query#format via the %1$s tokens (referenced twice, once per table).
    String placeholders = String.join(", ", Collections.nCopies(_commentIds.length, "?"));
    return "SELECT" +
        " p.comment_id, p.is_edited," +
        " r.source_kind, r.pubmed_id, r.external_url, r.external_title, r.pdf_content_sha256," +
        " r.ai_headline, r.ai_content" +
        " FROM %1$s." + Table.COMMENT_AI_PROVENANCE + " p" +
        " JOIN %1$s." + Table.COMMENT_AI_RUN + " r ON p.run_job_id = r.job_id" +
        " WHERE p.comment_id IN (" + placeholders + ")";
  }

  @Override
  protected Map<Long, AiProvenanceView> parseResults(ResultSet rs) throws SQLException {
    Map<Long, AiProvenanceView> out = new LinkedHashMap<>();
    while (rs.next()) {
      long commentId = rs.getLong("comment_id");

      AiProvenanceView.Source source = "pubmed".equals(rs.getString("source_kind"))
          ? AiProvenanceView.Source.pubmed(rs.getString("pubmed_id"))
          : AiProvenanceView.Source.upload(
              rs.getString("external_url"),
              rs.getString("external_title"),
              rs.getString("pdf_content_sha256"));

      out.put(commentId, new AiProvenanceView(
          rs.getBoolean("is_edited"),
          source,
          nullToEmpty(rs.getString("ai_headline")),
          nullToEmpty(rs.getString("ai_content"))));
    }
    return out;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  @Override
  protected Object[] getParams() {
    return _commentIds;
  }

  @Override
  protected Integer[] getTypes() {
    Integer[] types = new Integer[_commentIds.length];
    Arrays.fill(types, BIGINT);
    return types;
  }
}
