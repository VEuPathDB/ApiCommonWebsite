package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

public class PathwaysEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(PathwaysEnrichmentPlugin.class);

  static final String PATHWAY_BASE_URL_PROP_KEY = "pathwayPageUrl";

  public static final String PVALUE_PARAM_KEY = "pValueCutoff";
  public static final String PATHWAYS_SRC_PARAM_KEY = "pathwaysSources";
  
  public static final String TABBED_RESULT_FILE_PATH = "pathwaysEnrichmentResult.tab";
  
  public static final ResultRow HEADER_ROW = new ResultRow(
							   "Pathway ID", "Pathway Name", "Genes in the bkgd with this pathway","Genes in your result with this pathway", "Percent of bkgd Genes in your result", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni");

  public static final ResultRow COLUMN_HELP = new ResultRow(
      "Pathway ID",
      "Pathway Name",
      "Number of genes in this pathway in the background",
      "Number of genes in this pathway in your result",
      "Percentage of genes in the background in this pathway that are present in your result",
      "The percent of genes in this pathway in your result divided by the percent of genes in this pathway in the background",
      "Odds ratio statistic from the Fisher's exact test",
      "P-value from Fisher's exact test",
      "Benjamini-Hochberg false discovery rate (FDR)",
      "Bonferroni adjusted p-value"
  );

  @Override
  public ValidationErrors validateFormParams(Map<String, String[]> formParams) throws WdkModelException, WdkUserException {

    ValidationErrors errors = new ValidationErrors();

    // validate pValueCutoff
    validatePValue(formParams, errors);

    // validate annotation sources 
    String sourcesStr = getArrayParamValueAsString(PATHWAYS_SRC_PARAM_KEY, formParams, errors);

    validateFilteredPathways(sourcesStr, errors);

    return errors;
  }

  static void validatePValue(Map<String, String[]> formParams, ValidationErrors errors) {
    if (!formParams.containsKey(PVALUE_PARAM_KEY)) {
      errors.addParamMessage(PVALUE_PARAM_KEY, "Missing required parameter.");
    }
    else {
      try {
        float pValueCutoff = Float.parseFloat(formParams.get(PVALUE_PARAM_KEY)[0]);
        if (pValueCutoff <= 0 || pValueCutoff > 1) throw new NumberFormatException();
      }
      catch (NumberFormatException e) {
        errors.addParamMessage(PVALUE_PARAM_KEY, "Must be a number between greater than 0 and less than or equal to 1.");
      }
    }
  }

  private void validateFilteredPathways(String sourcesStr, ValidationErrors errors) throws WdkModelException, WdkUserException {

    String countColumn = "CNT";
    String idSql = getAnswerValue().getIdSql();
    String sql = 
"SELECT count (distinct pn.display_label) as " + countColumn + NL +
      "from   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec, " + NL +
      "dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga, " + NL +
      "apidb.pathwaynode pn," + NL +
      "(" + idSql + ") r" + NL +
      "where  ga.na_feature_id = t.parent_id" + NL +
      "AND    t.na_feature_id = taf.na_feature_id" + NL +
      "AND    taf.aa_sequence_id = asec.aa_sequence_id" + NL +
      "AND    asec.enzyme_class_id = ec.enzyme_class_id" + NL +
      "and    pn.display_label = ec.ec_number" + NL +
      "and    ga.source_id = r.source_id";

    LOG.info(sql);
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);

    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() < 1) {
      errors.addMessage("Your result has no genes with Pathways that satisfy the parameter choices you have made.  Please try adjusting the parameters.");
    }
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get(PVALUE_PARAM_KEY)[0];
    String sourcesStr = getArrayParamValueAsString(PATHWAYS_SRC_PARAM_KEY, params, null); // in sql format

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiPathwaysEnrichment").toString();
    LOG.info(qualifiedExe + " " + resultFilePath.toString() + " " + idSql + " " + 
			 wdkModel.getProjectId() + " " + pValueCutoff);
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql, wdkModel.getProjectId(), pValueCutoff,
			 sourcesStr};
  }

  /**
   * Make sure only one organism is represented in the results of this step
   * 
   * @param answerValue answerValue that will be passed to this step
   * @throws WdkUserException 
   * @throws IllegalAnswerException if more than one organism is represented in this answer
   */
  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException, WdkUserException {
    
    String countColumn = "CNT";
    String idSql = answerValue.getIdSql();
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    // check for non-zero count of genes with Pathways
    String sql = "SELECT count (distinct ga.source_id) as " + countColumn + NL +
      "from  sres.enzymeClass ec, dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga, apidb.pathwaynode pn, (" + idSql + ") r" + NL +
      "where  ga.aa_sequence_id = asec.aa_sequence_id" + NL +
      "AND    asec.enzyme_class_id = ec.enzyme_class_id" + NL +
      "and    (ec.ec_number LIKE REPLACE(pn.display_label,'-', '%') OR pn.display_label LIKE REPLACE(ec.ec_number,'-', '%'))" + NL +
      "and    ga.source_id = r.source_id";

    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);
    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() == 0 ) {
      throw new IllegalAnswerValueException("Your result has no genes that are in pathways, so you can't use this tool on this result. " +
          "Please revise your search and try again.");
    }

    // check for single organism
    sql = "SELECT count(distinct ga.taxon_id) as " + countColumn + NL +
        "FROM ApidbTuning.GeneAttributes ga,"  + NL +
        "(" + idSql + ") r"  + NL +
        "where ga.source_id = r.source_id";

    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    result = handler.getResults().get(0);
    count = (BigDecimal)result.get(countColumn);

    if (count.intValue() > 1) {
      throw new IllegalAnswerValueException("Your result has genes from more than " +
          "one organism.  The Pathways Enrichment analysis only accepts gene " +
          "lists from one organism.  Please use the Filter boxes to limit your " +
          "result to a single organism and try again.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException {
    
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    //String idSql = getAnswerValue().getIdSql();
    
    // find annotation sources used in the result set
    String sql = "select 'KEGG' as source from dual";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> sources = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      sources.add(cols.get("SOURCE").toString());
    }

    return new FormViewModel(sources);
  }
  
  @Override
  public Object getResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      if (buffer.ready()) buffer.readLine();  // throw away header line	
      while (buffer.ready()) {
        String line = buffer.readLine();
        String[] columns = line.split(TAB);
        results.add(new ResultRow(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5], columns[6], columns[7], columns[8], columns[9]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams(), getProperty(PATHWAY_BASE_URL_PROP_KEY));
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {
    
    private List<String> _sourceOptions;
    
    public FormViewModel(List<String> sourceOptions) {
      _sourceOptions = sourceOptions;
    }

    public List<String> getSourceOptions() {
      return _sourceOptions;
    }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    private String _pathwayBaseUrl;
    
    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String[]> formParams, String pathwayBaseUrl) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = resultData;
      _pathwayBaseUrl = pathwayBaseUrl;
    }

    public ResultRow getHeaderRow() { return PathwaysEnrichmentPlugin.HEADER_ROW; }
    public ResultRow getHeaderDescription() { return PathwaysEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return _formParams.get(PathwaysEnrichmentPlugin.PVALUE_PARAM_KEY)[0]; }
    public String getPathwaysSources() { return FormatUtil.join(_formParams.get(PathwaysEnrichmentPlugin.PATHWAYS_SRC_PARAM_KEY), ", "); }
    public String getPathwayBaseUrl() { return _pathwayBaseUrl; }
  }
  
  public static class ResultRow {
    
    private String _pathwayId;
    private String _pathwayName;
    private String _bgdGenes;
    private String _resultGenes;
    private String _percentInResult;
    private String _foldEnrich;
    private String _oddsRatio;
    private String _pValue;
    private String _benjamini;
    private String _bonferroni;

    public ResultRow(String pathwayId, String pathwayName, String bgdGenes, String resultGenes, String percentInResult, String foldEnrich, String oddsRatio, String pValue, String benjamini, String bonferroni) {
      _pathwayId = pathwayId;
      _pathwayName = pathwayName;
      _bgdGenes = bgdGenes;
      _resultGenes = resultGenes;
      _percentInResult = percentInResult;
      _foldEnrich = foldEnrich;
      _oddsRatio = oddsRatio;
      _pValue = pValue;
      _benjamini = benjamini;
      _bonferroni = bonferroni;
    }

    public String getPathwayId() { return _pathwayId; }
    public String getPathwayName() { return _pathwayName; }
    public String getBgdGenes() { return _bgdGenes; }
    public String getResultGenes() { return _resultGenes; }
    public String getPercentInResult() { return _percentInResult; }
    public String getFoldEnrich() { return _foldEnrich; }
    public String getOddsRatio() { return _oddsRatio; }
    public String getPvalue() { return _pValue; }
    public String getBenjamini() { return _benjamini; }
    public String getBonferroni() { return _bonferroni; }
  }
}
