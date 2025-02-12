package org.apidb.apicommon.model.report.ai.expression;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExperimentProcessor {
    private static final Set<String> KEYS_TO_KEEP = Set.of(
	"y_axis", "description", "genus_species", "project_id", "summary", "dataset_id",
        "assay_type", "x_axis", "module", "dataset_name", "display_name", "short_attribution", "paralog_number"
    );


    public static List<JSONObject> processExpressionData(ExpressionData expressionData) {
	return processExpressionData(expressionData, 0);
    }

    // for debugging only
    public static List<JSONObject> processExpressionData(ExpressionData expressionData, String datasetId) {
        List<JSONObject> experiments = processExpressionData(expressionData, 0);
        return experiments.stream()
                .filter(experiment -> datasetId.equals(experiment.getString("dataset_id")))
                .collect(Collectors.toList());
    }

    // maxExperiments is for dev/debugging only
    public static List<JSONObject> processExpressionData(ExpressionData expressionData, int maxExperiments) {
        List<JSONObject> experiments = new ArrayList<>();

        for (JSONObject expressionGraph : expressionData.getExpressionGraphs()) {
            String datasetId = expressionGraph.getString("dataset_id");

            // Extract only relevant keys from expressionGraph
            JSONObject experimentInfo = new JSONObject();
            for (String key : KEYS_TO_KEEP) {
                if (expressionGraph.has(key)) {
                    experimentInfo.put(key, expressionGraph.get(key));
                }
            }

            // Filter expressionGraphsDataTable to match dataset_id
            List<JSONObject> filteredData = new ArrayList<>();
            for (JSONObject entry : expressionData.getExpressionGraphsDataTable()) {
                if (datasetId.equals(entry.getString("dataset_id"))) {
                    JSONObject dataEntry = new JSONObject();
                    dataEntry.put("sample_name", entry.getString("sample_name"));
                    dataEntry.put("value", entry.get("value"));
                    if (entry.has("standard_error")) {
                        dataEntry.put("standard_error", entry.get("standard_error"));
                    }
                    if (entry.has("percentile_channel1")) {
                        dataEntry.put("percentile_channel1", entry.get("percentile_channel1"));
                    }
                    if (entry.has("percentile_channel2")) {
                        dataEntry.put("percentile_channel2", entry.get("percentile_channel2"));
                    }
                    filteredData.add(dataEntry);
                }
            }

            // Combine and store experiment data
	    experimentInfo.put("data", filteredData);
	    experiments.add(experimentInfo);
	    
	    if (maxExperiments > 0 && experiments.size() >= maxExperiments) {
		break;
	    }
        }

        return experiments;
    }
}
