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
      String ontologyValue = row.getAttributeValue("ontology").getValue();
      String goId = row.getAttributeValue("go_id").getValue();
      String evidenceCode = row.getAttributeValue("evidence_code").getValue();
      annotations.put(new JSONObject()
        .put("goTerm", goId)
        .put("goTermLabel", goId)
        .put("aspect", getAspect(ontologyValue))
        .put("geneRelationship", getRoValue(ontologyValue))
        .put("evidenceCode", evidenceCode)
        .put("evidenceCodeLabel", evidenceCode)
        .put("negate", row.getAttributeValue("is_not").getValue()));
    }
    return new JSONObject().put("go_annotations", annotations);
  }

  private static String getAspect(String ontologyValue) {
    switch (ontologyValue) {
      case "Biological Process": return "BP";
      case "Cellular Component": return "CC";
      case "Molecular Function": return "MF";
      default: return ontologyValue;
    }
  }

  private static String getRoValue(String ontologyValue) {
    switch (ontologyValue) {
      case "Biological Process": return "RO:0002331";
      case "Cellular Component": return "RO:0002432";
      case "Molecular Function": return "RO:0002327";
      default: return ontologyValue;
    }
  }

}