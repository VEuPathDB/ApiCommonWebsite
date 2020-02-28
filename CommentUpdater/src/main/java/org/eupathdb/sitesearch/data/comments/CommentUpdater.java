package org.eupathdb.sitesearch.data.comments;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.solr.SolrRuntimeException;
import org.json.JSONObject;

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
    findDocumentsToUpdate().forEach( (idTuple) -> updateDocumentComment(idTuple) );
  }

  private List<RecordIdTuple> findDocumentsToUpdate() {
    
    // sql to find sorted (source_id, comment_id) tuples from userdb
    String sqlSelect = "select source_id, comment_target_id as record_type, comment_id " +
        "from apidb.textsearchablecomment tsc, " + _commentSchema + ".comments c " +
        "where tsc.comment_id = c.comment_id" +
        "order by record_type asc, source_id asc, comment_id asc"; 
    
    return new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> {
        Response solrResponse = null;
        try {
          // get similar info from solr
          solrResponse = getSolrResponse();
          BufferedReader solrData = new BufferedReader(new InputStreamReader((InputStream)solrResponse.getEntity()));

          // compare the streams to find differences
          return findStaleDocuments(solrData, rs);
        }
        finally {
          if (solrResponse != null) solrResponse.close();
        }
      });
  }

  private List<RecordIdTuple> findStaleDocuments(BufferedReader solrData, ResultSet rs) {
    
    return new ArrayList<>();
  }

  private Response getSolrResponse() {
    String searchUrlSubpath = "/select" +
        "?q=MULTITEXT__gene_UserCommentContent:*" +    // any document with user comments
        "&fl=id,wdkPrimaryKeyString,userCommentIds" +  // output fields
        "&rows=1000000" +                              // row count: infinite
        "&wt=csv" +                                    // output format csv
        "&sort=wdkPrimaryKeyString desc";              // sorting
    Client client = ClientBuilder.newClient();
    String finalUrl = _solrUrl + searchUrlSubpath;
    LOG.info("Querying SOLR with: " + finalUrl);
    WebTarget webTarget = client.target(finalUrl);
    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
    Response response = invocationBuilder.get();
    if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
      throw new RuntimeException("SOLR responded with error");
    }
    return response;
  }

  private void updateDocumentComment(RecordIdTuple idTuple) {
    DocumentCommentsInfo comments = getCorrectCommentsForOneDocument(idTuple, _commentDb.getDataSource());
    JSONObject updateJson = new JSONObject(); 
    updateJson.put("id", idTuple.recordType + "__" + idTuple.sourceId);
    updateJson.put("userCommentIds", comments.commentIds);
    updateJson.put("UserCommentContent", comments.commentContents);
    updateSolrDocument(updateJson);
  }
  
  private DocumentCommentsInfo getCorrectCommentsForOneDocument(RecordIdTuple idTuple, DataSource commentDbDataSource) {
    
    String sqlSelect = "select comment_id, content " +
        "from apidb.textsearchablecomment " +
        "where source_id = '" + idTuple.sourceId + "'"; 
    
    return new SQLRunner(commentDbDataSource, sqlSelect)
      .executeQuery(rs -> {
        DocumentCommentsInfo comments = new DocumentCommentsInfo();
        while (rs.next()) {
          comments.commentIds.add(rs.getInt("comment_id"));
          comments.commentContents.add(rs.getString("content"));
        }
        return comments;
      });
  }

  private void updateSolrDocument(JSONObject jsonBody) {
    Response response = null;
    try {
      String urlSubpath = "/update";  
      Client client = ClientBuilder.newClient();
      String finalUrl = _solrUrl + urlSubpath;
      WebTarget webTarget = client.target(finalUrl);
      Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
      response = invocationBuilder.post(Entity.entity(jsonBody.toString(), MediaType.APPLICATION_JSON));
      if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        throw new SolrRuntimeException("Failed to execute SOLR update. " + response.getEntity().toString());
      }
    }
    finally {
      if (response != null) response.close();
    }
  }
  
  public class RecordIdTuple {
    String recordType;
    String sourceId;
  }
  
  public class DocumentCommentsInfo { 
    public DocumentCommentsInfo() {
      commentIds = new ArrayList<Integer>();
      commentContents = new ArrayList<String>();
    }
    List<Integer> commentIds;
    List<String> commentContents;
  }
}
