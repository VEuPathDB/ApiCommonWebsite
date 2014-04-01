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
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.analysis.IllegalStepException;

public class DummyGoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(DummyGoEnrichmentPlugin.class);

  private static final String[][] DUMMY_RESULT = {
    { ".8", "GO:0016310", "120", "103", "phosphorylation" },
    { ".71", "GO:0016772", "254", "139", "transferase activity, transferring phosphorus-containing groups" },
    { ".53", "GO:0016301", "154", "135", "kinase activity" },
    { ".50", "GO:0006796", "152", "104", "phosphate-containing compound metabolic process" },
    { ".49", "GO:0006793", "152", "104", "phosphorus metabolic process" },
    { ".47", "GO:0000166", "980", "117", "nucleotide binding" },
    { ".41", "GO:0016773", "142", "126", "phosphotransferase activity, alcohol group as acceptor" }
  };
  
  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    
    public ResultViewModel(String downloadPath, Map<String, String[]> formParams) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = new ArrayList<>();
      for (String[] row : DUMMY_RESULT) {
        _resultData.add(new ResultRow(row[0], row[1], row[2], row[3], row[4]));
      }
    }

    public ResultRow getHeaderRow() { return GoEnrichmentPlugin.HEADER_ROW; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return _formParams.get(GoEnrichmentPlugin.PVALUE_PARAM_KEY)[0]; }
    public String getGoSources() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_SRC_PARAM_KEY), ", "); }
  }
  
  @Override
  public Map<String,String> validateFormParams(Map<String, String[]> formParams) {
    return GoEnrichmentPlugin.validateParams(formParams);
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {
    return new String[]{ "/usr/bin/tee" };
  }
  
  @Override
  protected InputStream getProvidedInput() {
    StringBuilder tabbedData = new StringBuilder();
    for (String[] row : DUMMY_RESULT) {
      tabbedData
        .append(row[0]).append(TAB)
        .append(row[1]).append(TAB)
        .append(row[2]).append(TAB)
        .append(row[3]).append(TAB)
        .append(row[4]).append(TAB)
        .append(NL);
    }
    return IoUtil.getStreamFromString(tabbedData.toString());
  }

  @Override
  protected String getStdoutFileName() {
    return "enrichmentResults.xls";
  }

  @Override
  public void preApproveStep(Step step) throws IllegalStepException {
    if (!step.isBoolean()) {
      throw new IllegalStepException("We're sorry.  This analysis is only available on boolean steps.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException {
    return new FormViewModel();
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    return new ResultViewModel(getStdoutFileName(), getFormParams());
  }

}
