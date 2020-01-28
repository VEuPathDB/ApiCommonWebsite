package org.eupathdb.sitesearch.data.comments;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

    // compare maps and insert changes into SOLR
    List<String> genesToReload = findGenesToReload();

    // reload genes' comments
    reloadGeneComments(genesToReload);
  }

  private List<String> findGenesToReload() {
    String sqlSelect = ""; // TODO: create search SQL
    return new SQLRunner(_commentDb.getDataSource(), sqlSelect)
      .executeQuery(rs -> {
        Response solrResponse = null;
        try {
          solrResponse = getSolrResponse();
          BufferedReader solrData = new BufferedReader(new InputStreamReader((InputStream)solrResponse.getEntity()));
          return findBadGenes(solrData, rs);
        }
        finally {
          if (solrResponse != null) solrResponse.close();
        }
      });
  }

  private List<String> findBadGenes(BufferedReader solrData, ResultSet rs) {
    
    return new ArrayList<>();
  }

  private Response getSolrResponse() {
    String searchUrlSubpath = ""; // TODO: create search URL
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
