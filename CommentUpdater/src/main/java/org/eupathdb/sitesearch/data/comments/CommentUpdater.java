package org.eupathdb.sitesearch.data.comments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.gusdb.fgputil.solr.Solr;
import org.gusdb.fgputil.solr.SolrResponse;
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

    // read comment IDs already in SOLR
    Map<String,List<Long>> commentsInSolr = loadFromSolr();

    // read comment IDs from comment DB
    Map<String,List<Long>> commentsInDb = loadFromCommentDb();

    // compare maps and insert changes into SOLR
    List<String> genesToReload = getGenesToReload(commentsInSolr, commentsInDb);

    // reload genes' comments
    reloadGeneComments(genesToReload);
  }

  private Map<String, List<Long>> loadFromSolr() {
    Solr solr = new Solr(_solrUrl);
    String searchUrlSubpath = ""; // TODO: create search URL
    return solr
      .executeQuery(searchUrlSubpath, true, resp -> {
        SolrResponse response = Solr.parseResponse(searchUrlSubpath, resp);
        Map<String,List<Long>> map = new LinkedHashMap<>();
        // TODO: populate map from search results
        return map;
      });
  }

  private Map<String, List<Long>> loadFromCommentDb() {
    String sqlSelect = ""; // TODO: create search SQL
    return new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> {
        Map<String,List<Long>> map = new LinkedHashMap<>();
        // TODO: populate map from query results
        return map;
      });
  }

  private List<String> getGenesToReload(
      Map<String, List<Long>> commentsInSolr,
      Map<String, List<Long>> commentsInDb) {
    // TODO: find differences
    return null;
  }

  private void reloadGeneComments(List<String> genesToReload) {
    Response response = null;
    try {
      String urlSubpath = ""; // TODO: add POST endpoint
      Client client = ClientBuilder.newClient();
      String finalUrl = _solrUrl + urlSubpath;
      WebTarget webTarget = client.target(finalUrl);
      Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
      JSONObject json = new JSONObject(); // TODO: fill in with POST payload
      response = invocationBuilder.post(Entity.entity(json.toString(), MediaType.APPLICATION_JSON));
      if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
        throw new SolrRuntimeException("Failed to execute SOLR update. " + response.getEntity().toString());
      }
    }
    finally {
      if (response != null) response.close();
    }
  }

}
