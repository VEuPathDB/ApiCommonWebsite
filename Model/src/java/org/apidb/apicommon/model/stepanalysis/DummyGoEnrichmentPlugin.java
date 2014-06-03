package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.stepanalysis.GoEnrichmentPlugin.FormViewModel;
import org.apidb.apicommon.model.stepanalysis.GoEnrichmentPlugin.ResultRow;
import org.apidb.apicommon.model.stepanalysis.GoEnrichmentPlugin.ResultViewModel;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

public class DummyGoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(DummyGoEnrichmentPlugin.class);

  private static final String[][] DUMMY_RESULT = {
    { ".80", "GO:0016310", "120", "103", "phosphorylation", "85.8" },
    { ".71", "GO:0016772", "254", "139", "transferase activity, transferring phosphorus-containing groups", "54.7" },
    { ".53", "GO:0016301", "154", "135", "kinase activity", "87.7" },
    { ".50", "GO:0006796", "152", "104", "phosphate-containing compound metabolic process", "68.4" },
    { ".49", "GO:0006793", "152", "104", "phosphorus metabolic process", "68.4" },
    { ".47", "GO:0000166", "980", "117", "nucleotide binding", "11.9" },
    { ".41", "GO:0016773", "142", "126", "phosphotransferase activity, alcohol group as acceptor", "88.7" }
  };
  
  public static final List<String> DUMMY_ARRAY_OPTIONS =
      new ListBuilder<String>().add("GeneDB").add("InterproScan").toList();
  
  private ResultRow dummyArrayToResultRow(String[] row) {
    return new ResultRow(row[1], row[4], row[2], row[3], row[5], row[0], row[0], row[0], row[0], row[0]);
  }
  
  @Override
  public ValidationErrors validateFormParams(Map<String, String[]> formParams) {
    ValidationErrors errors = new ValidationErrors();
    GoEnrichmentPlugin.validatePValue(formParams, errors);
    // check for >1 selected value for each of the array input types
    GoEnrichmentPlugin.getArrayParamValueAsString(GoEnrichmentPlugin.GO_ASSOC_SRC_PARAM_KEY, formParams, errors);
    GoEnrichmentPlugin.getArrayParamValueAsString(GoEnrichmentPlugin.GO_EVID_CODE_PARAM_KEY, formParams, errors);
    GoEnrichmentPlugin.getArrayParamValueAsString(GoEnrichmentPlugin.GO_ASSOC_ONTOLOGY_PARAM_KEY, formParams, errors);
    return errors;
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {
    return new String[]{ "/usr/bin/tee" };
  }
  
  @Override
  protected InputStream getProvidedInput() {
    StringBuilder tabbedData = new StringBuilder();
    for (String[] row : DUMMY_RESULT) {
      ResultRow rd = dummyArrayToResultRow(row);
      tabbedData
        .append(rd.getGoId()).append(TAB)
        .append(rd.getGoTerm()).append(TAB)
        .append(rd.getPvalue()).append(TAB)
        .append(rd.getBgdGenes()).append(TAB)
        .append(rd.getResultGenes()).append(TAB)
        .append(rd.getPercentInResult()).append(TAB)
        .append(NL);
    }
    return IoUtil.getStreamFromString(tabbedData.toString());
  }

  @Override
  protected String getStdoutFileName() {
    return "enrichmentResults.tab";
  }

  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {
    if (answerValue.getResultSize() == 0) {
      throw new IllegalAnswerValueException("We're sorry. This analysis cannot be run on empty results.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException {
    return new FormViewModel(DUMMY_ARRAY_OPTIONS, DUMMY_ARRAY_OPTIONS, DUMMY_ARRAY_OPTIONS);
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    List<ResultRow> results = new ArrayList<>();
    for (String[] row : DUMMY_RESULT) {
      results.add(dummyArrayToResultRow(row));
    }
    return new ResultViewModel(getStdoutFileName(), results, getFormParams(),
        getProperty(GoEnrichmentPlugin.GO_TERM_BASE_URL_PROP_KEY));
  }

}
