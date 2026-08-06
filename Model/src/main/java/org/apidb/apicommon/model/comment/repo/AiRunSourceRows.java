package org.apidb.apicommon.model.comment.repo;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apidb.apicommon.model.comment.pojo.AiRunSource;
import org.apidb.apicommon.model.comment.pojo.ExternalRefKind;
import org.apidb.apicommon.model.comment.pojo.SourceKind;

/**
 * Maps the {@code source_kind}/{@code pubmed_id}/{@code external_*}/
 * {@code pdf_content_sha256} columns of a {@code comment_ai_run} row (or a join
 * onto it) into the sealed {@link AiRunSource} union. Shared by the two read
 * queries so the pubmed-vs-upload branch is written once.
 */
final class AiRunSourceRows {

  private AiRunSourceRows() {}

  /** Build the source union from the current row's source columns. */
  static AiRunSource fromResultSet(ResultSet rs) throws SQLException {
    return SourceKind.PUBMED == SourceKind.fromWire(rs.getString("source_kind"))
        ? new AiRunSource.Pubmed(rs.getString("pubmed_id"))
        : new AiRunSource.Upload(
            rs.getString("pdf_content_sha256"),
            rs.getString("external_url"),
            rs.getString("external_title"),
            rs.getString("external_ref"),
            ExternalRefKind.fromWire(rs.getString("external_ref_kind")));
  }
}
