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
import java.math.BigDecimal;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);

  public static final String PVALUE_PARAM_KEY = "pValueCutoff";
  public static final String GO_EVID_CODE_PARAM_KEY = "goEvidenceCodes";
  public static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";
  public static final String GO_ASSOC_ONTOLOGY_PARAM_KEY = "goAssociationsOntologies";
  
  public static final String TABBED_RESULT_FILE_PATH = "goEnrichmentResult.tab";
  
  public static final ResultRow HEADER_ROW = new ResultRow(
      "GO ID", "GO Term", "P-Value", "All Genes<br/>With This Term", "Result Genes<br/>With This Term", "Percent of All in This Result");

  public static final ResultRow COLUMN_HELP = new ResultRow(
      "Gene Ontology ID",
      "Gene Ontology Term",
      "P-Value Significance of the enrichment",
      "Number of genes with this term in this organism",
      "Number of genes with this term in your result",
      "Percent of genes in the organism that have this term that are present in your result"
  );

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
    
    // validate evidence codes
    String [] evidCodes = formParams.get(GO_EVID_CODE_PARAM_KEY);
    if (sources == null || sources.length == 0) {
      errors.put(GO_EVID_CODE_PARAM_KEY, "Missing required parameter.");
    }
    
    // validate ontology
    String [] ontologies = formParams.get(GO_ASSOC_ONTOLOGY_PARAM_KEY);
    if (ontologies == null || ontologies.length != 1) {
      errors.put(GO_ASSOC_ONTOLOGY_PARAM_KEY, "Missing required parameter, or more than one provided.");
    }
  
    return errors;
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get(PVALUE_PARAM_KEY)[0];
    String sourcesStr = "'" + FormatUtil.join(params.get(GO_ASSOC_SRC_PARAM_KEY), "','") + "'";  // in sql format
    String evidCodesStr = "'" + FormatUtil.join(params.get(GO_EVID_CODE_PARAM_KEY), "','") + "'";  // in sql format
    String ontology = params.get(GO_ASSOC_ONTOLOGY_PARAM_KEY)[0];

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiGoEnrichment").toString();
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql, wdkModel.getProjectId(), pValueCutoff,
			 ontology, sourcesStr, evidCodesStr };
  }

  /**
   * Make sure only one organism is represented in the results of this step
   * 
   * @param answerValue answerValue that will be passed to this step
   * @throws IllegalAnswerException if more than one organism is represented in this answer
   */
  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {
    
    String countColumn = "CNT";
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
    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() > 1) {
      throw new IllegalAnswerValueException("Your result has genes from more than " +
      		"one organism.  The GO Enrichment analysis only accepts gene " +
      		"lists from one organism.  Please use filters to limit your " +
      		"result to a single organism and try again.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException {
    
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    String idSql = getAnswerValue().getIdSql();
    
    // find annotation sources used in the result set
    String sql = "select distinct gts.source" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> sources = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      sources.add(cols.get("SOURCE").toString());
    }

    // find ontologies used in the result set
    sql = "select distinct gts.ontology" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id and gts.ontology is not null";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> ontologies = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      ontologies.add(cols.get("ONTOLOGY").toString());
    }
    
    // find evidence codes used in the result set
    sql = "select distinct gts.evidence_code" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> evidCodes = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      evidCodes.add(cols.get("EVIDENCE_CODE").toString());
    }

    return new FormViewModel(sources, ontologies, evidCodes);
  }
  
  @Override
  public Object getResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      while (buffer.ready()) {
        String line = buffer.readLine();
        String[] columns = line.split(TAB);
        results.add(new ResultRow(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams());
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {
    
    private List<String> _sourceOptions;
    private List<String> _ontologyOptions;
    private List<String> _evidCodeOptions;
    
    public FormViewModel(List<String> sourceOptions, List<String> ontologyOptions, List<String> evidCodeOptions) {
      _sourceOptions = sourceOptions;
      _ontologyOptions = ontologyOptions;
      _evidCodeOptions = evidCodeOptions;
    }

    public List<String> getSourceOptions() {
      return _sourceOptions;
    }

    public List<String> getEvidCodeOptions() {
      return _evidCodeOptions;
    }

    public List<String> getOntologyOptions() {
      return _ontologyOptions;
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
    public ResultRow getHeaderDescription() { return GoEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return _formParams.get(GoEnrichmentPlugin.PVALUE_PARAM_KEY)[0]; }
    public String getGoSources() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_SRC_PARAM_KEY), ", "); }
    public String getEvidCodes() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_EVID_CODE_PARAM_KEY), ", "); }
    public String getGoOntologies() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_ONTOLOGY_PARAM_KEY), ", "); }
  }
  
  public static class ResultRow {
    
    private String _pValue;
    private String _goId;
    private String _bgdGenes;
    private String _resultGenes;
    private String _goTerm;
    private String _percentInResult;

    public ResultRow(String goId, String goTerm, String pValue, String bgdGenes, String resultGenes, String percentInResult) {
      _pValue = pValue;
      _goId = goId;
      _bgdGenes = bgdGenes;
      _resultGenes = resultGenes;
      _goTerm = goTerm;
      _percentInResult = percentInResult;
    }

    public String getPvalue() { return _pValue; }
    public String getGoId() { return _goId; }
    public String getBgdGenes() { return _bgdGenes; }
    public String getResultGenes() { return _resultGenes; }
    public String getGoTerm() { return _goTerm; }
    public String getPercentInResult() { return _percentInResult; }
  }
}
