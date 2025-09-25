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
 * NOTE: this was updated to also deal with comments coming from Apollo.  Thus the
 * abstraction.
 *
 * @author Steve
 */

public abstract class CommentUpdater<IDTYPE> {

  private static final Logger LOG = Logger.getLogger(CommentUpdater.class);

  private final String _solrUrl;

  private final DatabaseInstance _commentDb;

  private final String _commentSchema;
  
  private final CommentSolrDocumentFields _docFields;
  
  private final CommentUpdaterSql _updaterSql;

  protected CommentUpdater(
    final String           solrUrl,
    final DatabaseInstance commentDb,
    final String           commentSchema,
    final CommentSolrDocumentFields docFields,
    final CommentUpdaterSql updaterSql
  ) {
    _solrUrl = solrUrl;
    _commentDb = commentDb;
    _commentSchema = commentSchema + (commentSchema.endsWith(".") ? "" : ".");
    _docFields = docFields;
    _updaterSql = updaterSql;
  }
  
  DatabaseInstance getCommentDb() { return _commentDb; }
  String getCommentSchema() { return _commentSchema; }

  public void syncAll() {
    try {
      findDocumentsToUpdate().forEach(this::updateDocumentComment);
    } finally {
      solrCommit();
    }
  }
  
  /**
   * Get an in-memory list of record IDs for those records that are out of date
   * in Solr
   */
  private List<SolrDocument> findDocumentsToUpdate() {

    // sql to find sorted (source_id, comment_id) tuples from userdb
    var sqlSelect = _updaterSql.getSortedCommentsSql(_commentSchema);

    var results = new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> findStaleDocuments(fetchCommentedRecords(), rs));

    // for source IDs that have comments, but for which no solr document that has comments was found,
    // attempt to fetch the corresponding solr document.  (either the document exists, and does not
    // have comments, or the document does not exist because the source ID is an alias).
    // add the fetched documents to the update list
    results.toUpdate.addAll(
      fetchDocumentsById(results.toPostProcess
        .stream()
        .map(RecordInfo::toSolrId)
        .toArray(String[]::new)).values());
    
    LOG.info("Total documents to update: " + results.toUpdate.size() + "  To postprocess: " + results.toPostProcess.size());

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
    final Map<String, SolrDocument> solrData,
    final ResultSet      rs
  ) {
    var dbRow = new RecordInfo();
    var out   = new ProcessingResult();

    LOG.info("Finding stale documents");

    // Set of already invalidated source ids.  Used to skip
    // rows when possible
    var tmp = new HashSet<String>();
    int count = 0;
    try {
      outer:
      while (rs.next()) {
        count++;
        dbRow.readRs(rs);

        // if the current source id was added to the tmp map
        // then we don't need to bother with any of it's
        // other rows.
        while (tmp.contains(dbRow.sourceId)) {
          if (!rs.next())
            break outer;
          count++;
          dbRow.readRs(rs);
        }

        // save for post-processing source IDs that did not have a parallel document with a comment
        // (might possibly be on old source ID, ie, alias, for which no document exists)
        // as a post-process we will synthesize a solr ID for these, based on source ID, fetch the doc, and add to update list
        if (!solrData.containsKey(dbRow.sourceId)) {
          out.toPostProcess.add(dbRow.copy());
          tmp.add(dbRow.sourceId);
          continue;
        }

        // save for updating documents that do have comments, but are missing at least one
        var doc = solrData.get(dbRow.sourceId);
        if (!doc.hasCommentId(dbRow.commentId)) {
          out.toUpdate.add(doc);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    int missing = out.toUpdate.size();

    LOG.info("Read " + count + " (source_id, record_type, comment_id) tuples from database");

     // add to update list solr documents that have comments that were deleted in db
    solrData.values()
      // For each document
      .stream()
      // that is not already queued for update
      .filter(d -> !tmp.contains(d.getSourceId()))
      // that has referenced comments not appearing in the db
      .filter(SolrDocument::hasUnhitComments)
      // queue for update
      .forEach(out.toUpdate::add);
    
    int delete = out.toUpdate.size() - missing;

    LOG.info("Found " + out.toPostProcess.size() + " source IDs with no corresponding solr document having comments, " 
    + missing + " documents that need updated comments and " + delete + " documents whose comments have all been deleted" );

    return out;
  }

  /**
   * Retrieves documents from Solr with id values matching
   * the given input ids.
   *
   * @param ids Solr IDs for documents to lookup
   *
   * @return Map of WDK SourceID to Solr {@link SolrDocument}
   */
  Map<String, SolrDocument> fetchDocumentsById(final String[] ids) {
    LOG.info("Attempting to fetch " + ids.length + " solr documents by (putative) ID.  (Will not be found if ID is an alias)");

    var q = new SolrTermQueryBuilder(_docFields.getIdFieldName())
      .values(ids)
      .resultFields(_docFields.getRequiredFields())
      .maxRows(1000000)
      .resultFormat(FormatType.CSV);

    Map<String, SolrDocument> docs = 
    fetchDocuments(
      _solrUrl + (_solrUrl.endsWith("/") ? "select" : "/select"),
      Entity.entity(q.toString(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)
    );
    // LOG.info("Fetched " + docs.size() + " documents with query: " + q);

    return docs;
  }

  /**
   * Retrieves documents from Solr that have existing
   * comment content data.
   *
   * @return Map of WDK SourceID to Solr {@link SolrDocument}
   */
  private Map<String, SolrDocument> fetchCommentedRecords() {

    SolrUrlQueryBuilder builder = SolrUrlQueryBuilder.select(_solrUrl)
      .filterAndAllOf(_docFields.getCommentContentFieldName());
 
    builder = applyOptionalSolrFilters(builder)
      .resultFields(_docFields.getRequiredFields())
      .maxRows(1000000)
      .resultFormat(FormatType.CSV);

    Map<String, SolrDocument> docs = fetchDocuments(builder.buildQuery(), null);

    LOG.info("Found " + docs.size() + " solr documents that have one or more existing comments");

    return docs;
  }

  abstract SolrUrlQueryBuilder applyOptionalSolrFilters(SolrUrlQueryBuilder builder);

  /**
   * For a given record, get up-to-date comment info from the database.  Format
   * into JSON to be submitted as a document update to solr.
   *
   * This method formats a solr document ID, rather than getting it from Solr.
   * We do this because the Solr stream might not include documents without
   * comments, and so would not provide those IDs.
   */
  void updateDocumentComment(SolrDocument doc) {
    var comments = getCorrectCommentsForOneSourceId(doc.getSourceId(), _commentDb.getDataSource(), _commentSchema);

    LOG.info("Updating source ID '" + doc.getSourceId() + "' to have comments with IDs: " + 
        comments.commentIds.stream().map(IDTYPE::toString).collect(Collectors.joining(",")));

    var updateJson = new JSONArray().put(
      new JSONObject()
        .put(_docFields.getIdFieldName(), doc.getSolrId()) // concoct a valid solr unique ID
        .put(_docFields.getBatchTypeFieldName(), doc.getDocumentType())
        .put(_docFields.getBatchIdFieldName(), doc.getBatchId())
        .put(_docFields.getBatchNameFieldName(), doc.getBatchName())
        .put(_docFields.getBatchTypeFieldName(), doc.getBatchType())
        .put(_docFields.getBatchTimeFieldName(), doc.getBatchTime())
        .put(_docFields.getCommentIdFieldName(), new JSONObject().put("set", comments.commentIds))
        .put(_docFields.getCommentContentFieldName(), new JSONObject().put("set", comments.commentContents))
    );

    updateSolrDocument(updateJson);
  }

  /**
   * Get the up-to-date comments info from the database, for the provided wdk
   * record
   */
  abstract DocumentCommentsInfo<IDTYPE> getCorrectCommentsForOneSourceId(
    final String sourceId,
    final DataSource commentDbDataSource,
    final String commentSchema
  );

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

  void solrCommit() {
    Response res = null;
    try {
      res = sendSolrRequest(_solrUrl + (_solrUrl.endsWith("/") ? "" : "/") + "update?commit=true", null);
    } finally {
      if (res != null)
        res.close();
    }
  }

  private static Map<String, SolrDocument> fetchDocuments(String url, Entity<?> body) {
    Response res = null;
    try {
      res = sendSolrRequest(url, body);

      var read = entityReader(res);
      if (!read.ready()) {
        LOG.info("Read not ready when attempting to fetch documents");
        return Collections.emptyMap();
      }

      // Skip first line (headers)
      read.readLine();

      Map<String, SolrDocument> docMap = read.lines()
        .map(SolrDocument::readCsvRow)
        .collect(Collectors.toMap(SolrDocument::getSourceId, Function.identity()));
      
      LOG.info("Read " + docMap.size() + " documents from solr");
      
      return docMap;


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
        + out.getStatus() + " " + out.readEntity(String.class));
    }

    return out;
  }

  /**
   * Models comment info found in a single Solr document
   *
   * @author Steve
   */
  static class DocumentCommentsInfo<IDTYPE> {
    List<IDTYPE> commentIds = new ArrayList<>();
    List<String> commentContents = new ArrayList<>();
  }
}
