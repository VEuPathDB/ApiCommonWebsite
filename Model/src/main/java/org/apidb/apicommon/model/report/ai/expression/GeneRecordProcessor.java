package org.apidb.apicommon.model.report.ai.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gusdb.fgputil.EncryptionUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.TableValueRow;
import org.json.JSONObject;

/**
 * expects a geneRecord with two tables: "ExpressionGraphs" and "ExpressionGraphsDataTable"
 *
 * returns a list of JSON Objects of data ready to feed the AI
 */
public class GeneRecordProcessor {

  private static final Set<String> KEYS_TO_KEEP = Set.of("y_axis", "description", "genus_species",
      "project_id", "summary", "dataset_id", "assay_type", "x_axis", "module", "dataset_name", "display_name",
      "short_attribution", "paralog_number");

  private static final String EXPRESSION_GRAPH_TABLE = "ExpressionGraphs";
  private static final String EXPRESSION_GRAPH_DATA_TABLE = "ExpressionGraphsDataTable";

  public static final List<String> REQUIRED_TABLE_NAMES = List.of(EXPRESSION_GRAPH_TABLE, EXPRESSION_GRAPH_DATA_TABLE);

  // Increment this to invalidate all previous cache entries:
  // (for example if changing first level model outputs rather than inputs which are already digestified)
  private static final String DATA_MODEL_VERSION = "v3";
  
  public interface ExperimentInputs {

    String getCacheKey();

    String getDatasetId();

    String getAssayType();

    String getExperimentName();

    String getDigest();

    JSONObject getExperimentData();
  }

  public interface GeneSummaryInputs {

    String getGeneId(); // is the cache key

    List<ExperimentInputs> getExperimentsWithData();

    String getDigest();
  }

  private static String getGeneId(RecordInstance record) {
    return record.getPrimaryKey().getValues().get("source_id");
  }

  public static GeneSummaryInputs getSummaryInputsFromRecord(RecordInstance record, String aiChatModel, Function<JSONObject, String> getExperimentPrompt, Function<List<JSONObject>, String> getFinalSummaryPrompt) throws WdkModelException {

    String geneId = getGeneId(record);

    List<ExperimentInputs> experimentsWithData = GeneRecordProcessor.processExpressionData(record, aiChatModel, getExperimentPrompt, 0);

    return new GeneSummaryInputs() {
      @Override
      public String getGeneId() {
        return geneId;
      }

      @Override
      public List<ExperimentInputs> getExperimentsWithData() {
        return experimentsWithData;
      }

      @Override
      public String getDigest() {
        // Instead of building the final summary prompt using the AI-generated **summary outputs**
        // (which happens during real processing), we construct it using JSON-encoded MD5
        // **digests** of the per-experiment **inputs**.
        //
        // This avoids fetching per-experiment results from the cache while remaining
        // functionally identical for cache validation purposes.
        List<JSONObject> digests = experimentsWithData.stream()
            .map(exp -> new JSONObject().put("digest", exp.getDigest()))
            .collect(Collectors.toList());
        return EncryptionUtil.md5(aiChatModel + ":" + DATA_MODEL_VERSION + ":" + getFinalSummaryPrompt.apply(digests));
      }

    };
  }

  private static List<ExperimentInputs> processExpressionData(RecordInstance record, String aiChatModel, Function<JSONObject, String> getExperimentPrompt, int maxExperiments) throws WdkModelException {
    try {
      // return value:
      List<ExperimentInputs> experiments = new ArrayList<>();

      String geneId = getGeneId(record);
      TableValue expressionGraphs = record.getTableValue(EXPRESSION_GRAPH_TABLE);
      TableValue expressionGraphsDataTable = record.getTableValue(EXPRESSION_GRAPH_DATA_TABLE);

      for (TableValueRow experimentRow : expressionGraphs) {

        JSONObject experimentInfo = new JSONObject();

        // Extract all relevant attributes
        for (String key : KEYS_TO_KEEP) {
          experimentInfo.put(key, experimentRow.getAttributeValue(key).getValue());
        }

        String datasetId = experimentRow.getAttributeValue("dataset_id").getValue();
        String assayType = experimentRow.getAttributeValue("assay_type").getValue();
        String experimentName = experimentRow.getAttributeValue("display_name").getValue();

        List<JSONObject> filteredData = readFilteredData(datasetId, expressionGraphsDataTable); 

        experimentInfo.put("data", filteredData);

        experiments.add(new ExperimentInputs() {

          @Override
          public String getDatasetId() {
            return datasetId;
          }
	    
          @Override
          public String getAssayType() {
            return assayType;
          }
	    
          @Override
          public String getExperimentName() {
            return experimentName;
          }

          @Override
          public String getCacheKey() {
            return geneId + ':' + datasetId;
          }

          @Override
          public String getDigest() {
            return EncryptionUtil.md5(aiChatModel + ":" + DATA_MODEL_VERSION + ":" + getExperimentPrompt.apply(getExperimentData()));
          }

          @Override
          public JSONObject getExperimentData() {
            return experimentInfo;
          }
        });

        if (maxExperiments > 0 && experiments.size() >= maxExperiments)
          break;
      }
      return experiments;
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e.getMessage());
    }
  }

  private static List<JSONObject> readFilteredData(String datasetId, TableValue expressionGraphsDataTable) throws WdkModelException, WdkUserException {
    List<JSONObject> filteredData = new ArrayList<>();
    // add data from `expressionGraphsDataTable` where attribute "dataset_id" equals `datasetId`
    //   (this would be more efficient with a `Map<String, List<TableValueRow>>` made before the `expressionGraphs` loop)
    //List<TableValueRow> thisExperimentDataRows = new ArrayList<>();
    for (TableValueRow dataRow : expressionGraphsDataTable) {
      if (dataRow.getAttributeValue("dataset_id").getValue().equals(datasetId)) {
        JSONObject dataEntry = new JSONObject();

        // Extract relevant numeric fields
        List<String> dataKeys = List.of("value", "standard_error", "percentile_channel1", "percentile_channel2", "sample_name");
        for (String key : dataKeys) {
          dataEntry.put(key, dataRow.getAttributeValue(key).getValue());
        }
        filteredData.add(dataEntry);
      }
    }
    return filteredData;
  }

}
