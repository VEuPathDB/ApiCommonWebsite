package org.apidb.apicommon.model.report.ai.expression;

import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.TableValueRow;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkModelException;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * expects a geneRecord with two tables: "ExpressionGraphs" and "ExpressionGraphsDataTable"
 *
 * returns a list of JSON Objects of data ready to feed the AI
 */

public class GeneRecordProcessor {
  private static final Set<String> KEYS_TO_KEEP =
    Set.of(
           "y_axis", "description", "genus_species", "project_id", "summary", "dataset_id",
           "assay_type", "x_axis", "module", "dataset_name", "display_name", "short_attribution", "paralog_number"
           );

  public static List<JSONObject> processExpressionData(RecordInstance geneRecord) throws WdkModelException, WdkUserException {
    return processExpressionData(geneRecord, 0);
  }

  // for debugging only
  public static List<JSONObject> processExpressionData(RecordInstance geneRecord, String datasetId) throws WdkModelException, WdkUserException {
    List<JSONObject> experiments = processExpressionData(geneRecord, 0);
    return experiments.stream()
      .filter(experiment -> datasetId.equals(experiment.getString("dataset_id")))
      .collect(Collectors.toList());
  }

  // maxExperiments is for dev/debugging only
  public static List<JSONObject> processExpressionData(RecordInstance geneRecord, int maxExperiments) throws WdkModelException, WdkUserException {
    // return value:
    List<JSONObject> experiments = new ArrayList<>();

    String geneId = geneRecord.getAttributeValue("gene_id").getValue();
    TableValue expressionGraphs = geneRecord.getTableValue("ExpressionGraphs");
    TableValue expressionGraphsDataTable = geneRecord.getTableValue("ExpressionGraphsDataTable");

    for (TableValueRow experimentRow : expressionGraphs) {
      JSONObject experimentInfo = new JSONObject();
      experimentInfo.put("gene_id", geneId);
      
      // Extract all relevant attributes
      for (String key : KEYS_TO_KEEP) {
        experimentInfo.put(key, experimentRow.getAttributeValue(key).getValue());
      }

      List<JSONObject> filteredData = new ArrayList<>();
      String datasetId = experimentRow.getAttributeValue("dataset_id").getValue();
      // add data from `expressionGraphsDataTable` where attribute "dataset_id" equals `datasetId`
      // (this would be more efficient with a `Map<String, List<TableValueRow>>` made before the `expressionGraphs` loop)
      List<TableValueRow> thisExperimentDataRows = new ArrayList<>();
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

      experimentInfo.put("data", filteredData);
      experiments.add(experimentInfo);
    
      if (maxExperiments > 0 && experiments.size() >= maxExperiments) break;
    }
    return experiments;
  }
}
