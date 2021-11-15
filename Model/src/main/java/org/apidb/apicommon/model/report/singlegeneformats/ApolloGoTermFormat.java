package org.apidb.apicommon.model.report.singlegeneformats;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    Set<String> seenKeys = new HashSet<>();
    for (TableValueRow row : recordInstance.getTableValue(GO_TERM_TABLE_NAME)) {
      String ontologyValue = row.getAttributeValue("ontology").getValue();
      String goId = row.getAttributeValue("go_id").getValue();
      String evidenceCode = row.getAttributeValue("evidence_code").getValue();
      String negateValue = "false";
      String noteArray = "[]";
      String goLabel = row.getAttributeValue("go_term_name").getValue();
      String key = String.join("|", goId, goLabel, ontologyValue, evidenceCode);
      if (seenKeys.add(key)) {
        annotations.put(new JSONObject()
          .put("goTerm", goId)
          .put("goTermLabel", goLabel)
          .put("aspect", getAspect(ontologyValue))
          .put("geneRelationship", getRoValue(ontologyValue))
          .put("evidenceCode", getEcCode(evidenceCode))
          .put("evidenceCodeLabel", evidenceCode)
          .put("negate", negateValue )
          .put("notes", noteArray));
      }
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

  private static String getEcCode(String evidenceCode) {
    switch (evidenceCode) {
      case "EXP": return "ECO:0000269";
      case "HDA": return "ECO:0007005";
      case "HEP": return "ECO:0007007";
      case "HGI": return "ECO:0007003";
      case "HMP": return "ECO:0007001";
      case "HTP": return "ECO:0006056";
      case "IBA": return "ECO:0000318";
      case "IBD": return "ECO:0000319";
      case "IC": return "ECO:0000305";
      case "IDA": return "ECO:0000314";
      case "IEA": return "ECO:0000501";
      case "IEP": return "ECO:0000270";
      case "IGC": return "ECO:0000317";
      case "IGI": return "ECO:0000316";
      case "IKR": return "ECO:0000320";
      case "IMP": return "ECO:0000315";
      case "IPI": return "ECO:0000353";
      case "IRD": return "ECO:0000321";
      case "ISA": return "ECO:0000247";
      case "ISM": return "ECO:0000255";
      case "ISO": return "ECO:0000266";
      case "ISS": return "ECO:0000250";
      case "NAS": return "ECO:0000303";
      case "ND": return "ECO:0000307";
      case "RCA": return "ECO:0000245";
      case "TAS": return "ECO:0000304";
      default: return evidenceCode;
    }
  }

}
