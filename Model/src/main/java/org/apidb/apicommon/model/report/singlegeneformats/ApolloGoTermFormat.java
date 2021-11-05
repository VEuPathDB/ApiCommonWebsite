package org.apidb.apicommon.model.report.singlegeneformats;

import java.util.Collections;
import java.util.List;

import org.apidb.apicommon.model.report.SingleGeneReporter.Format;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValueRow;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApolloGoTermFormat implements Format {

  private static final String GO_TERM_TABLE_NAME = "GOTerms";

  @Override
  public List<String> getRequiredAttributeNames() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getRequiredTableNames() {
    return List.of(GO_TERM_TABLE_NAME);
  }

  @Override
  public JSONObject writeJson(RecordInstance recordInstance) throws WdkModelException, WdkUserException {
    JSONArray annotations = new JSONArray();
    for (TableValueRow row : recordInstance.getTableValue(GO_TERM_TABLE_NAME)) {
      annotations.put(new JSONObject()
        .put("goTerm", row.getAttributeValue("ontology").getValue())
        .put("evidenceCodeLabel", row.getAttributeValue("evidence_code_parameter").getValue())
        .put("negate", row.getAttributeValue("is_not").getValue())
        .put("goTermLabel", row.getAttributeValue("go_term_name").getValue())
        .put("evidenceCode", row.getAttributeValue("evidence_code").getValue())
        .put("id", row.getAttributeValue("go_id").getValue()));
    }
    return new JSONObject().put("go_annotations", annotations);
  }

}
