package org.apidb.apicommon.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.User;

import org.apache.log4j.Logger;

/**
 * Transform an input PK (potentially old) into zero, one or more valid transcript PKs
 * by:
 *   1) mapping old gene ID to new gene IDs
 *   2) if an invalid transcript ID is provided, replace it with an arbitrary valid one per new gene ID
 * @author steve
 *
 */
public class TranscriptPrimaryKeyAliasPlugin implements org.gusdb.wdk.model.record.PrimaryKeyAliasPlugin {
  private static final Logger logger = Logger.getLogger(TranscriptPrimaryKeyAliasPlugin.class);

  @Override
  public List<Map<String, Object>> getPrimaryKey(User user, Map<String, Object> inputPkValues)
      throws WdkModelException, WdkUserException {

    if (!inputPkValues.containsKey("gene_source_id")) {
      throw new WdkUserException("Requesting Gene page, but no Gene Id supplied");
    }

    String inputGeneId = (String) inputPkValues.get("gene_source_id");
    String inputTranscriptId = inputPkValues.containsKey("source_id")
        ? (String) inputPkValues.get("source_id") : null;

    // get the mapped genes, keyed by their transcripts (sorted alphabetically)
    SortedMap<String, String> transcriptToGene = findCurrentIds(inputGeneId, user.getWdkModel().getAppDb().getDataSource());

    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

    // if asking for a default transcript
    // for each gene found, return it, along with its alphabetically first transcript
    if (inputTranscriptId.equals("_DEFAULT_")) {
      String prevGene = "";
      for (String transcript : transcriptToGene.keySet()) {
        String gene = transcriptToGene.get(transcript);
        if (!prevGene.equals(gene)) {
          result.add(getPkMap(gene, transcript, user.getWdkModel().getProjectId()));
          prevGene = gene;
        }
      }
    }

    // otherwise, just map the gene to new gene id, and go w/ the provided transcript id (and a prayer that is not old)
    else {
      String gene = transcriptToGene.get(inputTranscriptId);
      if (gene != null) result.add(getPkMap(gene, inputTranscriptId, user.getWdkModel().getProjectId()));
    }

    return result;
  }
  
  private Map<String, Object> getPkMap(String gene, String transcript, String projectId) {
    Map<String, Object> pk = new HashMap<String, Object>();
    pk.put("gene_source_id", gene);
    pk.put("source_id", transcript);
    pk.put("project_id", projectId);
    return pk;
  }
  
  /**
   * @param inputGeneId the gene id supplied by the user
   * @return map of each new transcript id to its new gene id
   * @throws WdkModelException 
   */
  private SortedMap<String,String> findCurrentIds(String inputGeneId, DataSource appDbDataSource) throws WdkModelException {
    String sql = "SELECT ta.source_id, ta.gene_source_id "
        + "FROM ApidbTuning.GeneId a, ApidbTuning.TranscriptAttributes ta "
        + "WHERE a.id = '" + inputGeneId + "' "
        + "AND ta.gene_source_id = a.gene";
        
    SortedMap<String,String> map = new TreeMap<String, String>();
    ResultSet resultSet = null;
    try {
      resultSet = SqlUtils.executeQuery(appDbDataSource, sql, "Transcript-Primary-Key-Plugin");
      while (resultSet.next()) {
        map.put(resultSet.getString("SOURCE_ID"), resultSet.getString("GENE_SOURCE_ID"));
      }
    } catch (SQLException e) {
      throw new WdkModelException(e);
    } finally {
      if (resultSet != null) SqlUtils.closeResultSetAndStatement(resultSet);
    }
    return map;
  }

}
