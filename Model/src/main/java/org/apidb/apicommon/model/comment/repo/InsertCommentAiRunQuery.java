package org.apidb.apicommon.model.comment.repo;

import static java.sql.Types.ARRAY;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apidb.apicommon.model.comment.pojo.CommentAiRun;
import org.gusdb.fgputil.db.runner.ArgumentBatch;
import org.gusdb.fgputil.db.runner.ListArgumentBatch;

/**
 * Insert one row into {@code comment_ai_run} — the shared LLM-output cache,
 * keyed by the content-digest {@code job_id}. Idempotent at the application
 * level: the digest dedupes resubmissions, so a row is written at most once per
 * distinct job.
 *
 * <p>Follows the {@link InsertCategoryQuery} / {@link InsertAttachmentQuery}
 * pattern. {@code job_id} is the supplied primary key (no sequence), so the
 * {@link IdSupplier} is {@code null}.
 *
 * <p>The {@code synonyms_used TEXT[]} column is bound as a real PostgreSQL array
 * built via {@link Connection#createArrayOf}, which is why {@link #run(Connection)}
 * is overridden to capture the connection before {@link #getArguments()} runs.
 */
public class InsertCommentAiRunQuery extends InsertQuery {

  private static final String SQL = "INSERT INTO %s." + Table.COMMENT_AI_RUN + " (" +
      "job_id, model_name, prompt_version, source_kind, pubmed_id, " +
      "external_url, external_title, pdf_content_sha256, gene_id, synonyms_used, " +
      "options_json, terminal_status, is_only_mentioned_in_passing, " +
      "ai_headline, ai_content, completed_at, external_ref, external_ref_kind" +
      ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final Integer[] TYPES = {
      VARCHAR,    // job_id
      VARCHAR,    // model_name
      VARCHAR,    // prompt_version
      VARCHAR,    // source_kind
      VARCHAR,    // pubmed_id
      VARCHAR,    // external_url
      VARCHAR,    // external_title
      VARCHAR,    // pdf_content_sha256
      VARCHAR,    // gene_id
      ARRAY,      // synonyms_used
      VARCHAR,    // options_json
      VARCHAR,    // terminal_status
      BOOLEAN,    // is_only_mentioned_in_passing
      VARCHAR,    // ai_headline
      VARCHAR,    // ai_content
      TIMESTAMP,  // completed_at
      VARCHAR,    // external_ref
      VARCHAR     // external_ref_kind
  };

  private final CommentAiRun _run;
  private Connection _con;

  public InsertCommentAiRunQuery(String schema, CommentAiRun run) {
    super(schema, Table.COMMENT_AI_RUN, null);
    _run = run;
  }

  @Override
  public Query run(Connection con) throws SQLException {
    _con = con;  // captured so getArguments() can build the TEXT[] array
    return super.run(con);
  }

  @Override
  protected ArgumentBatch getArguments() throws SQLException {
    Array synonyms = _con.createArrayOf("text", _run.getSynonymsUsed().toArray());
    Timestamp completedAt = _run.getCompletedAt() == null ? null
        : new Timestamp(_run.getCompletedAt().getTime());

    ListArgumentBatch batch = new ListArgumentBatch();
    batch.add(new Object[] {
        _run.getJobId(), _run.getModelName(), _run.getPromptVersion(),
        _run.getSourceKind(), _run.getPubmedId(), _run.getExternalUrl(),
        _run.getExternalTitle(), _run.getPdfContentSha256(), _run.getGeneId(),
        synonyms, _run.getOptionsJson(), _run.getTerminalStatus(),
        _run.isOnlyMentionedInPassing(), _run.getAiHeadline(),
        _run.getAiContent(), completedAt,
        _run.getExternalRef(), _run.getExternalRefKind()
    });
    batch.setParameterTypes(TYPES);
    return batch;
  }

  @Override
  protected String getQuery() {
    return SQL;
  }
}
