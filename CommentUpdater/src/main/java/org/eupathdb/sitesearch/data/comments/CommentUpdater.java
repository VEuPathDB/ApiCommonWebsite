package org.eupathdb.sitesearch.data.comments;

import org.apache.log4j.Logger;
import org.eupathdb.sitesearch.data.comments.solr.FormatType;
import org.eupathdb.sitesearch.data.comments.solr.SolrTermQueryBuilder;
import org.eupathdb.sitesearch.data.comments.solr.SolrUrlQueryBuilder;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sql.DataSource;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/***
 * Find all documents in Solr with out-of-date user comment fields relative to
 * the comment database.  Update each such document in Solr with current comment
 * data.
 *
 * Solr documents will have two user comment fields, both multi-valued.  Every
 * comment associated with the document will have a value in each field
 *   - comment IDs
 *   - comment contents
 *
 * The strategy of this updater is to compare sorted streams of IDs from Solr
 * and the database.  When a difference is found, remember the document.
 *
 * Finally iterate through the list of stale documents and individually read the
 * full data from the database and update the document in Solr
 *
 * The design of this updater assumes that the vast majority of comments are
 * up-to-date in Solr
 * (having been written there when Solr was originally populated with documents).
 *
 * It is not intended to support large cardinality updates.
 *
 * @author Steve
 */
public class CommentUpdater {

  private static final Logger LOG = Logger.getLogger(CommentUpdater.class);

  private final String _solrUrl;

  private final DatabaseInstance _commentDb;

  private final String _commentSchema;

  public CommentUpdater(
    final String           solrUrl,
    final DatabaseInstance commentDb,
    final String           commentSchema
  ) {
    _solrUrl = solrUrl;
    _commentDb = commentDb;
    _commentSchema = commentSchema;
  }

  public void syncAll() {
    try {
      findDocumentsToUpdate().forEach(this::updateDocumentComment);
    } finally {
      solrCommit();
    }
  }

  public void updateSingle(final long commentId) {
    var schema = _commentSchema + (_commentSchema.endsWith(".") ? "" : ".");
    // Intentionally selecting dead comments for comment delete case.
    var select = "SELECT stable_id "
      + "FROM " + schema + "comments "
      + "WHERE comment_id = ?"
      + "UNION "
      + "SELECT stable_id "
      + "FROM " + schema + "commentstableid "
      + "WHERE comment_id = ?";
    var genes  = new SQLRunner(_commentDb.getDataSource(), select)
      .executeQuery(
        new Object[] {commentId, commentId},
        new Integer[] {Types.BIGINT, Types.BIGINT},
        rs -> {
          var tmp = new ArrayList<String>();
          while (rs.next()) {
            tmp.add("gene__" + rs.getString(1));
          }
          return tmp.toArray(new String[0]);
        });
    try {
      fetchDocumentsById(genes).values().forEach(this::updateDocumentComment);
    } finally {
      solrCommit();
    }
  }

  /**
   * Get an in-memory list of record IDs for those records that are out of date
   * in Solr
   */
  private List<DocumentInfo> findDocumentsToUpdate() {

    // sql to find sorted (source_id, comment_id) tuples from userdb
    var sqlSelect = "SELECT source_id, comment_target_type as record_type, c.comment_id"
      + " FROM apidb.textsearchablecomment tsc,"
      + " " + _commentSchema + "comments c "
      + " WHERE tsc.comment_id = c.comment_id"
      + "   AND project_id = 'PlasmoDB'"
      + " ORDER BY source_id DESC, c.comment_id";

    var results = new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> findStaleDocuments(fetchCommentedRecords(), rs));

    results.toUpdate.addAll(
      fetchDocumentsById(results.toPostProcess
        .stream()
        .map(RecordInfo::toSolrId)
        .toArray(String[]::new)).values());

    return results.toUpdate;
  }

  /**
   * Iterate through parallel streams, comparing them.
   *
   * The rows from the database are one per (recordId, commentId) tuple and are
   * already sorted.
   *
   * Example Result:
   * <code>
   *   SOURCE_ID,RECORD_TYPE,COMMENT_ID
   *   1MB.524,gene,19863
   *   AAEL01000103,gene,62820
   *   ACA1_086420,gene,100189863
   *   ACA1_171110,gene,100189943
   *   ACA1_175560,gene,100189873
   *   ACA1_182400,gene,100189823
   * </code>
   *
   * We assume that Solr and the database have the same set of records
   * (they better!).  Documents missing from the Solr stream are therefore
   * assumed to be present in Solr, but have no comments, and are therefore
   * in need of updating.
   */
  private ProcessingResult findStaleDocuments(
    final Map<String, DocumentInfo> solrData,
    final ResultSet      rs
  ) {
    var dbRow = new RecordInfo();
    var out   = new ProcessingResult();

    // Set of already invalidated source ids.  Used to skip
    // rows when possible
    var tmp = new HashSet<String>();
    try {
      outer:
      while (rs.next()) {
        dbRow.readRs(rs);

        // if the current source id was added to the tmp map
        // then we don't need to bother with any of it's
        // other rows.
        while (tmp.contains(dbRow.sourceId)) {
          if (!rs.next())
            break outer;
          dbRow.readRs(rs);
        }

        if (!solrData.containsKey(dbRow.sourceId)) {
          out.toPostProcess.add(dbRow.copy());
          tmp.add(dbRow.sourceId);
          continue;
        }

        var doc = solrData.get(dbRow.sourceId);
        if (!doc.hasCommentId(dbRow.commentId))
          out.toUpdate.add(doc);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    solrData.values()
      // For each document
      .stream()
      // that is not already queued for update
      .filter(d -> !tmp.contains(d.getSourceId()))
      // that has referenced comments not appearing in the db
      .filter(DocumentInfo::hasUnhitComments)
      // queue for update
      .forEach(out.toUpdate::add);

    return out;
  }

  /**
   * Retrieves documents from Solr with id values matching
   * the given input ids.
   *
   * @param ids Solr IDs for documents to lookup
   *
   * @return Map of WDK SourceID to Solr {@link DocumentInfo}
   */
  private Map<String, DocumentInfo> fetchDocumentsById(final String[] ids) {
    var q = new SolrTermQueryBuilder(Field.ID)
      .values(ids)
      .resultFields(DocumentInfo.REQUIRED_FIELDS)
      .maxRows(1000000)
      .resultFormat(FormatType.CSV);
    return fetchDocuments(
      _solrUrl + (_solrUrl.endsWith("/") ? "select" : "/select"),
      Entity.entity(q.toString(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)
    );
  }

  /**
   * Retrieves documents from Solr that have existing
   * comment content data.
   *
   * @return Map of WDK SourceID to Solr {@link DocumentInfo}
   */
  private Map<String, DocumentInfo> fetchCommentedRecords() {
    return fetchDocuments(SolrUrlQueryBuilder.select(_solrUrl)
      .filterAndAllOf(Field.COMMENT_TXT)
      .resultFields(DocumentInfo.REQUIRED_FIELDS)
      .maxRows(1000000)
      .resultFormat(FormatType.CSV)
      .buildQuery(), null);
  }

  /**
   * For a given record, get up-to-date comment info from the database.  Format
   * into JSON to be submitted as a document update to solr.
   *
   * This method formats a solr document ID, rather than getting it from Solr.
   * We do this because the Solr stream might not include documents without
   * comments, and so would not provide those IDs.
   */
  private void updateDocumentComment(DocumentInfo doc) {
    var comments = getCorrectCommentsForOneDocument(doc, _commentDb.getDataSource());

    var updateJson = new JSONArray().put(
      new JSONObject()
        .put(Field.ID, doc.getSolrId()) // concoct a valid solr unique ID
        .put(Field.DOC_TYPE, doc.getDocumentType())
        .put(Field.BATCH_ID, doc.getBatchId())
        .put(Field.BATCH_NAME, doc.getBatchName())
        .put(Field.BATCH_TYPE, doc.getBatchType())
        .put(Field.BATCH_TIME, doc.getBatchTime())
        .put(Field.COMMENT_ID, new JSONObject().put("set", comments.commentIds))
        .put(Field.COMMENT_TXT, new JSONObject().put("set", comments.commentContents))
    );

    updateSolrDocument(updateJson);
  }

  /**
   * Get the up-to-date comments info from the database, for the provided wdk
   * record
   */
  private DocumentCommentsInfo getCorrectCommentsForOneDocument(
    final DocumentInfo doc,
    final DataSource commentDbDataSource
  ) {

    var sqlSelect = " SELECT comment_id, content"
      + " FROM apidb.textsearchablecomment"
      + " WHERE source_id = '" + doc.getSourceId() + "'";

    return new SQLRunner(commentDbDataSource, sqlSelect)
      .executeQuery(rs -> {
        var comments = new DocumentCommentsInfo();

        while (rs.next()) {
          comments.commentIds.add(rs.getInt("comment_id"));
          comments.commentContents.add(rs.getString("content"));
        }

        return comments;
      });
  }

  /***
   * Apply a JSON update to Solr
   */
  private void updateSolrDocument(JSONArray jsonBody) {
    Response response = null;
    try {
      var finalUrl = _solrUrl + (_solrUrl.endsWith("/")
        ? "" : "/") + "update";

      response = sendSolrRequest(finalUrl, Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));

    }
    finally {
      if (response != null)
        response.close();
    }
  }

  private void solrCommit() {
    Response res = null;
    try {
      res = sendSolrRequest(_solrUrl + (_solrUrl.endsWith("/") ? "" : "/") + "update?commit=true", null);
    } finally {
      if (res != null)
        res.close();
    }
  }

  private static Map<String, DocumentInfo> fetchDocuments(String url, Entity<?> body) {
    Response res = null;
    try {
      res = sendSolrRequest(url, body);

      var read = entityReader(res);
      if (!read.ready())
        return Collections.emptyMap();

      // Skip first line (headers)
      read.readLine();

      return read.lines()
        .map(DocumentInfo::readCsvRow)
        .collect(Collectors.toMap(DocumentInfo::getSourceId, Function.identity()));

    } catch (IOException e) {
      throw new RuntimeException("Failed to read Solr response body", e);
    } finally {
      if (res != null)
        res.close();
    }
  }

  private static BufferedReader entityReader(final Response rs) {
    return new BufferedReader(new InputStreamReader(
      (InputStream)rs.getEntity()
    ));
  }

  private static Response sendSolrRequest(final String url, final Entity<?> payload) {
    Response out;

    var req = ClientBuilder.newClient()
      .target(url)
      .request();

    LOG.info(String.format("Making %s request to Solr at %s",
      payload == null ? "GET" : "POST", url));

    if (payload != null)
      out = req.post(payload);
    else
      out = req.get();

    if (!out.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
      throw new RuntimeException("SOLR responded with an error: "
        + out.getStatus() + out.readEntity(String.class));
    }

    return out;
  }

  /**
   * Models comment info found in a single Solr document
   *
   * @author Steve
   */
  static class DocumentCommentsInfo {
    List<Integer> commentIds = new ArrayList<>();
    List<String> commentContents = new ArrayList<>();
  }
}
