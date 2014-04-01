package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.xml.NamedValue;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);

  public static final String PVALUE_PARAM_KEY = "pValueCutoff";
  public static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";
  
  public static final String TABBED_RESULT_FILE_PATH = "goEnrichmentResult.tab";
  
  public static final ResultRow HEADER_ROW = new ResultRow(
      "P-Value", "GO ID", "Genes in Bgd", "Genes in Result", "GO Term");

  @Override
  public Map<String,String> validateFormParams(Map<String, String[]> formParams) {
    return validateParams(formParams);
  }
  
  public static Map<String,String> validateParams(Map<String, String[]> formParams) {
    Map<String,String> errors = new HashMap<String,String>();

    // validate pValueCutoff
    if (!formParams.containsKey(PVALUE_PARAM_KEY)) {
      errors.put(PVALUE_PARAM_KEY, "Missing required parameter.");
    }
    else {
      try {
        float pValueCutoff = Float.parseFloat(formParams.get(PVALUE_PARAM_KEY)[0]);
        if (pValueCutoff <= 0 || pValueCutoff > 1) throw new NumberFormatException();
      }
      catch (NumberFormatException e) {
        errors.put(PVALUE_PARAM_KEY, "Must be a number between greater than 0 and less than or equal to 1.");
      }
    }
    
    // validate annotation sources
    String [] sources = formParams.get(GO_ASSOC_SRC_PARAM_KEY);
    if (sources == null || sources.length == 0) {
      errors.put(GO_ASSOC_SRC_PARAM_KEY, "Missing required parameter.");
    }
  
    return errors;
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get(PVALUE_PARAM_KEY)[0];
    String sourcesStr = FormatUtil.join(params.get(GO_ASSOC_SRC_PARAM_KEY), ",");

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    return new String[]{ "apiGoEnrichment", idSql, pValueCutoff,
        resultFilePath.toString(), wdkModel.getProjectId(), sourcesStr };
  }

  /**
   * Make sure only one organism is represented in the results of this step
   * 
   * @param answerValue answerValue that will be passed to this step
   * @throws IllegalAnswerException if more than one organism is represented in this answer
   */
  @Override
  public void preApproveAnswer(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {
    
    String countColumn = "cnt";
    String idSql = answerValue.getIdSql();
    String sql = "SELECT count(distinct ga.taxon_id) as " + countColumn + NL +
        "FROM ApidbTuning.GeneAttributes ga,"  + NL +
        "(" + idSql + ") r"  + NL +
        "where ga.source_id = r.source_id";

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);
    Integer count = (Integer)result.get(countColumn);

    if (count > 1) {
      throw new IllegalAnswerValueException("Your result has genes from more than " +
      		"one organism.  The GO Enrichment analysis only accepts gene " +
      		"lists from one organism.  Please use filters to limit your " +
      		"result to a single organism and try again.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException {
    
    String sourceIdCol = "srcId";
    String sourceNameCol = "srcName";
    
    String sql = "select go_source_id as " + sourceIdCol +
        ", go_source_name as " + sourceNameCol + " from sources";
    
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql).executeQuery(handler);
    
    List<NamedValue> sources = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      sources.add(new NamedValue(
          cols.get(sourceIdCol).toString(),
          cols.get(sourceNameCol).toString()));
    }
    
    return new FormViewModel(sources);
  }
  
  @Override
  public Object getResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      while (buffer.ready()) {
        String line = buffer.readLine();
        String[] tokens = line.split(TAB);
        results.add(new ResultRow(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams());
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {
    
    private List<NamedValue> _sourceOptions;
    
    public FormViewModel(List<NamedValue> sourceOptions) {
      _sourceOptions = sourceOptions;
    }

    public List<NamedValue> getSourceOptions() {
      return _sourceOptions;
    }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    
    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String[]> formParams) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = resultData;
    }

    public ResultRow getHeaderRow() { return GoEnrichmentPlugin.HEADER_ROW; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return _formParams.get(GoEnrichmentPlugin.PVALUE_PARAM_KEY)[0]; }
    public String getGoSources() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_SRC_PARAM_KEY), ", "); }
  }
  
  public static class ResultRow {
    
    private String _pValue;
    private String _goId;
    private String _bgdGenes;
    private String _resultGenes;
    private String _goTerm;

    public ResultRow(String pValue, String goId, String bgdGenes, String resultGenes, String goTerm) {
      _pValue = pValue;
      _goId = goId;
      _bgdGenes = bgdGenes;
      _resultGenes = resultGenes;
      _goTerm = goTerm;
    }

    public String getPvalue() { return _pValue; }
    public String getGoId() { return _goId; }
    public String getBgdGenes() { return _bgdGenes; }
    public String getResultGenes() { return _resultGenes; }
    public String getGoTerm() { return _goTerm; }
  }
}
