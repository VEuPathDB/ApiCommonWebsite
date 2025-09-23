package org.apidb.apicommon.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.PrimaryKeyAliasPlugin;
import org.gusdb.wdk.model.record.RecordNotFoundException;
import org.gusdb.wdk.model.user.User;

/**
 * Transform an input PK (potentially old) into zero, one or more valid transcript PKs
 * by:
 *   1) mapping old gene ID to new gene IDs
 *   2) if an invalid transcript ID is provided, replace it with an arbitrary valid one per new gene ID
 * @author steve
 *
 */
public class GenePrimaryKeyAliasPlugin implements PrimaryKeyAliasPlugin {

  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(GenePrimaryKeyAliasPlugin.class);

  @Override
  public List<Map<String, Object>> getPrimaryKey(User user, Map<String, Object> inputPkValues)
      throws WdkModelException, RecordNotFoundException {

    if (!inputPkValues.containsKey("source_id")) {
      throw new WdkModelException("Requesting Gene page, but no Gene Id supplied");
    }

    String inputGeneId = (String) inputPkValues.get("source_id");

    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

    // TEMP.. just add my gene id here
    result.add(getPkMap(inputGeneId, user.getWdkModel()));

    // get the mapped genes, keyed by their transcripts (sorted alphabetically)
    //SortedMap<String, String> transcriptToGene = findCurrentIds(inputGeneId, user.getWdkModel().getAppDb().getDataSource());

    /*
    // if asking for a default transcript
    // then, for each gene found, return it, along with its alphabetically first transcript
    if (inputTranscriptId.equals("_DEFAULT_TRANSCRIPT_")) {
      String prevGene = "";
      for (String transcript : transcriptToGene.keySet()) {
        String gene = transcriptToGene.get(transcript);
        if (!prevGene.equals(gene)) {
          result.add(getPkMap(gene, transcript, user.getWdkModel()));
          prevGene = gene;
        }
      }
    }

    // otherwise, map the gene to new gene id, and go w/ the input transcript id (if it is old, it will cause user err)
    else {
      String gene = transcriptToGene.get(inputTranscriptId);
      if (gene != null) result.add(getPkMap(gene, inputTranscriptId, user.getWdkModel().getProjectId()));
    }
    */
    return result;
  }
  
  private Map<String, Object> getPkMap(String gene, WdkModel wdkModel) {
    return new MapBuilder<String, Object>()
      .put("source_id", gene)
      .putIf(TranscriptUtil.isProjectIdInPks(wdkModel), "project_id", wdkModel.getProjectId())
      .toMap();
  }
  
  /**
   * @param inputGeneId the gene id supplied by the user
   * @return map of each new transcript id to its new gene id
   * @throws WdkModelException 
   */
  @SuppressWarnings("unused")
  private SortedMap<String,String> findCurrentIds(String inputGeneId, DataSource appDbDataSource) throws WdkModelException {
    String sql = "SELECT ta.source_id, ta.gene_source_id "
        + "FROM webready.GeneId a, webready.TranscriptAttributes ta "
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
      if (resultSet != null) SqlUtils.closeResultSetAndStatement(resultSet, null);
    }
    return map;
  }

}
