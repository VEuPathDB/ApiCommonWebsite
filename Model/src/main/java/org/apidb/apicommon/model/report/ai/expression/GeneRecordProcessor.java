package org.apidb.apicommon.model.report.ai.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gusdb.fgputil.EncryptionUtil;
import org.gusdb.fgputil.json.JsonUtil;
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

  public interface GeneSummaryInputs {

    String getGeneId();

    List<JSONObject> getExperimentsWithData();

    default String getExperimentsDigest() {
      return EncryptionUtil.md5(getExperimentsWithData().stream()
          .map(JsonUtil::serialize).collect(Collectors.joining()));
    }
  }

  public static GeneSummaryInputs getSummaryInputsFromRecord(RecordInstance record) throws WdkModelException {
    String geneId = record.getPrimaryKey().getValues().get("gene_source_id");
    List<JSONObject> experimentsWithData = GeneRecordProcessor.processExpressionData(record);
    return new GeneSummaryInputs() {
      @Override
      public String getGeneId() {
        return geneId;
      }
      @Override
      public List<JSONObject> getExperimentsWithData() {
        return experimentsWithData;
      }
    };
  }

  static List<JSONObject> processExpressionData(RecordInstance geneRecord)
      throws WdkModelException {
    return processExpressionData(geneRecord, 0);
  }

  // for debugging only
  static List<JSONObject> processExpressionData(RecordInstance geneRecord, String datasetId) throws WdkModelException {
    List<JSONObject> experiments = processExpressionData(geneRecord, 0);
    return experiments.stream().filter(
        experiment -> datasetId.equals(experiment.getString("dataset_id"))).collect(Collectors.toList());
  }

  // maxExperiments is for dev/debugging only
  static List<JSONObject> processExpressionData(RecordInstance geneRecord, int maxExperiments)
      throws WdkModelException {
    try {
      // return value:
      List<JSONObject> experiments = new ArrayList<>();
  
      TableValue expressionGraphs = geneRecord.getTableValue("ExpressionGraphs");
      TableValue expressionGraphsDataTable = geneRecord.getTableValue("ExpressionGraphsDataTable");
  
      for (TableValueRow experimentRow : expressionGraphs) {
        JSONObject experimentInfo = new JSONObject();
  
        // Extract all relevant attributes
        for (String key : KEYS_TO_KEEP) {
          experimentInfo.put(key, experimentRow.getAttributeValue(key).getValue());
        }
  
        List<JSONObject> filteredData = new ArrayList<>();
        String datasetId = experimentRow.getAttributeValue("dataset_id").getValue();
        // add data from `expressionGraphsDataTable` where attribute "dataset_id" equals `datasetId`
        // (this would be more efficient with a `Map<String, List<TableValueRow>>` made before the
        // `expressionGraphs` loop)
        for (TableValueRow dataRow : expressionGraphsDataTable) {
          if (dataRow.getAttributeValue("dataset_id").getValue().equals(datasetId)) {
            JSONObject dataEntry = new JSONObject();
  
            // Extract relevant numeric fields
            List<String> dataKeys = List.of("value", "standard_error", "percentile_channel1",
                "percentile_channel2", "sample_name");
            for (String key : dataKeys) {
              dataEntry.put(key, dataRow.getAttributeValue(key).getValue());
            }
  
            filteredData.add(dataEntry);
          }
        }
  
        experimentInfo.put("data", filteredData);
        experiments.add(experimentInfo);
  
        if (maxExperiments > 0 && experiments.size() >= maxExperiments)
          break;
      }
      return experiments;
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e.getMessage());
    }
  }
}
