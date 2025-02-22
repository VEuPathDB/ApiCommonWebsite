package org.apidb.apicommon.model.report.ai.expression;

import org.json.JSONObject;
import java.util.List;

public class ExpressionData {
    private final List<JSONObject> expressionGraphs;
    private final List<JSONObject> expressionGraphsDataTable;

    public ExpressionData(List<JSONObject> expressionGraphs, List<JSONObject> expressionGraphsDataTable) {
        this.expressionGraphs = expressionGraphs;
        this.expressionGraphsDataTable = expressionGraphsDataTable;
    }

    public List<JSONObject> getExpressionGraphs() {
        return expressionGraphs;
    }

    public List<JSONObject> getExpressionGraphsDataTable() {
        return expressionGraphsDataTable;
    }
}
