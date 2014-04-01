package org.apidb.apicommon.model.stepanalysis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.stepanalysis.GoEnrichmentPlugin.FormViewModel;
import org.apidb.apicommon.model.stepanalysis.GoEnrichmentPlugin.ResultRow;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.xml.NamedValue;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;

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
  
  private static final String[][] DUMMY_RESULT_ORIG = {
    { "GO-7", "98" },
    { "GO-3", "87" },
    { "GO-5", "55" },
    { "GO-1", "34" },
    { "GO-2", "17" },
    { "GO-6", "12" },
    { "GO-8", "3" },
    { "GO-9", "2" },
    { "GO-4", "0" }
  };
  
  public static class ResultViewModel {

    private List<NamedValue> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    
    public ResultViewModel(String downloadPath, Map<String, String[]> formParams) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = new ArrayList<>();
      for (String[] row : DUMMY_RESULT_ORIG) {
        _resultData.add(new NamedValue(row[0], row[1]));
      }
    }

    public ResultRow getHeaderRow() { return GoEnrichmentPlugin.HEADER_ROW; }
    public List<NamedValue> getResultData() { return _resultData; }
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
    for (String[] row : DUMMY_RESULT_ORIG) {
      tabbedData.append(row[0] + "\t" + row[1] + FormatUtil.NL);
    }
    return IoUtil.getStreamFromString(tabbedData.toString());
  }

  @Override
  protected String getStdoutFileName() {
    return "enrichmentResults.xls";
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
