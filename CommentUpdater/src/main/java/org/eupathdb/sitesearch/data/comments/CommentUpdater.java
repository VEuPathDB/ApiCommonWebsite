package org.eupathdb.sitesearch.data.comments;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.solr.SolrRuntimeException;
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
import java.util.ArrayList;
import java.util.List;

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

  public CommentUpdater(String solrUrl, DatabaseInstance commentDb, String commentSchema) {
    _solrUrl = solrUrl;
    _commentDb = commentDb;
    _commentSchema = commentSchema;
  }

  public void performSync() {
    findDocumentsToUpdate()
      .forEach(this::updateDocumentComment);
  }

  /**
   * Get an in-memory list of record IDs for those records that are out of date in Solr
   * @return
   */
  private List<RecordIdTuple> findDocumentsToUpdate() {

    // sql to find sorted (source_id, comment_id) tuples from userdb
    var sqlSelect = "SELECT source_id, comment_target_type as record_type, c.comment_id"
      + " FROM apidb.textsearchablecomment tsc,"
      + " " + _commentSchema + ".comments c "
      + " WHERE tsc.comment_id = c.comment_id"
      + " ORDER BY record_type, source_id, comment_id";

    return new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> {
        Response solrResponse = null;
        try {
          // get similar info from solr
          solrResponse = getSolrResponse();
          var solrData = new BufferedReader(new InputStreamReader(
            (InputStream)solrResponse.getEntity()));

          // compare the streams to find differences
          return findStaleDocuments(solrData, rs);
        }
        finally {
          if (solrResponse != null)
            solrResponse.close();
        }
      });
  }

  /**
   * Iterate through parallel streams, comparing them.
   *
   * The rows from solr are one per document, sorted by wdkPrimaryKey, with the
   * comment IDs in a single cell.  Those comments to be serialized and sorted.
   *
   * Example Result:
   * <code>
   *   id,wdkPrimaryKeyString,userCommentIds
   *   gene__mal_mito_3,mal_mito_3,"102190,1137,1203"
   *   gene__mal_mito_2,mal_mito_2,102180
   *   gene__mal_mito_1,mal_mito_1,102170
   *   gene__PY17X_1464400,PY17X_1464400,100062223
   *   gene__PY17X_1463500,PY17X_1463500,79950
   * </code>
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
  private List<RecordIdTuple> findStaleDocuments(BufferedReader solrData, ResultSet rs) {
//    try {
//      solrData.readLine();
//      var tuple = RecordIdTuple.fromRs(rs);
//    } catch (IOException | SQLException e) {
//      e.printStackTrace();
//    }
//     TODO: write this method
    return new ArrayList<>();
  }

  /**
   * Get a csv response from Solr.
   *
   * The 3 columns are docId, wdkPrimaryKeyString,  and comma-delimited list of
   * commentIDs.
   *
   * Rows are sorted by wdkPrimaryKeyString.
   *
   * TODO: tune the solr query to omit documents that have empty user comment
   *       fields.
   */
  private Response getSolrResponse() {
    var searchUrl = _solrUrl + "/select" +
        "?q=MULTITEXT__gene_UserCommentContent:*" +    // any document with user comments
        "&fl=id,wdkPrimaryKeyString,userCommentIds" +  // output fields
        "&rows=1000000" +                              // row count: infinite
        "&wt=csv" +                                    // output format csv
        "&sort=wdkPrimaryKeyString desc";              // sorting

    LOG.info("Querying SOLR with: " + searchUrl);

    var response = ClientBuilder.newClient()
      .target(searchUrl)
      .request(MediaType.APPLICATION_JSON)
      .get();

    if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
      throw new RuntimeException("SOLR responded with error");
    }

    return response;
  }

  /**
   * For a given record, get up-to-date comment info from the database.  Format
   * into JSON to be submitted as a document update to solr.
   *
   * This method formats a solr document ID, rather than getting it from Solr.
   * We do this because the Solr stream might not include documents without
   * comments, and so would not provide those IDs.
   */
  private void updateDocumentComment(RecordIdTuple idTuple) {
    var comments = getCorrectCommentsForOneDocument(idTuple, _commentDb.getDataSource());

    var updateJson = new JSONObject()
      .put("id", idTuple.recordType + "__" + idTuple.sourceId) // concoct a valid solr unique ID
      .put("userCommentIds", comments.commentIds)
      .put("UserCommentContent", comments.commentContents);

    updateSolrDocument(updateJson);
  }

  /**
   * Get the up-to-date comments info from the database, for the provided wdk
   * record
   */
  private DocumentCommentsInfo getCorrectCommentsForOneDocument(RecordIdTuple idTuple, DataSource commentDbDataSource) {

    var sqlSelect = "select comment_id, content " +
        "from apidb.textsearchablecomment " +
        "where source_id = '" + idTuple.sourceId + "'";

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
  private void updateSolrDocument(JSONObject jsonBody) {
    Response response = null;
    try {
      var finalUrl = _solrUrl + "/update";

      response = ClientBuilder.newClient()
        .target(finalUrl)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));

      if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        throw new SolrRuntimeException("Failed to execute SOLR update. " + response.getEntity().toString());
      }
    }
    finally {
      if (response != null)
        response.close();
    }
  }

  /**
   * A tuple holding a record ID from the database (eg a Gene ID)
   *
   * @author Steve
   */
  static class RecordIdTuple {
    String recordType;
    String sourceId;

    static RecordIdTuple fromRs(ResultSet rs) throws SQLException {
      var out = new RecordIdTuple();
      out.recordType = rs.getString(1);
      out.sourceId = rs.getString(2);
      return out;
    }
  }

  /**
   * Models comment info found in a single Solr document
   *
   * @author Steve
   */
  public static class DocumentCommentsInfo {
    List<Integer> commentIds = new ArrayList<>();
    List<String> commentContents = new ArrayList<>();
  }
}
