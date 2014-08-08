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
import org.apidb.apicommon.model.stepanalysis.EnrichmentPluginUtil.Option;
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

  private static final String PATHWAY_BASE_URL_PROP_KEY = "pathwayPageUrl";
  private static final String PATHWAYS_SRC_PARAM_KEY = "pathwaysSources";

  private static final String TABBED_RESULT_FILE_PATH = "pathwaysEnrichmentResult.tab";
  
  private static final ResultRow HEADER_ROW = new ResultRow(
      "Pathway ID", "Pathway Name", "Genes in the bkgd with this pathway","Genes in your result with this pathway", "Percent of bkgd Genes in your result", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni");

  private static final ResultRow COLUMN_HELP = new ResultRow(
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
    EnrichmentPluginUtil.validatePValue(formParams, errors);

    // validate organism
    EnrichmentPluginUtil.validateOrganism(formParams, getAnswerValue(), getWdkModel(), errors);

    // validate annotation sources 
    String sourcesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        PATHWAYS_SRC_PARAM_KEY, formParams, errors);

    // only validate further if the above pass
    if (errors.isEmpty()) {
      validateFilteredPathways(sourcesStr, errors);
    }

    return errors;
  }

  // TODO: what affect does adding organism param have on this validation???
  private void validateFilteredPathways(String sourcesStr, ValidationErrors errors) throws WdkModelException, WdkUserException {

    String countColumn = "CNT";
    String idSql = getAnswerValue().getIdSql();
    String sql = 
        "SELECT count (distinct pn.display_label) as " + countColumn + NL +
        "FROM   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec, " + NL +
        "dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga, " + NL +
        "apidb.pathwaynode pn," + NL +
        "(" + idSql + ") r" + NL +
        "WHERE  ga.na_feature_id = t.parent_id" + NL +
        "AND    t.na_feature_id = taf.na_feature_id" + NL +
        "AND    taf.aa_sequence_id = asec.aa_sequence_id" + NL +
        "AND    asec.enzyme_class_id = ec.enzyme_class_id" + NL +
        "AND    pn.display_label = ec.ec_number" + NL +
        "AND    ga.source_id = r.source_id";

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
    Map<String,String[]> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);
    String sourcesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        PATHWAYS_SRC_PARAM_KEY, params, null); // in sql format

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
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException, WdkUserException {

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    // find annotation sources used in the result set
    String sql = "select 'KEGG' as source from dual";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> sources = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      sources.add(new Option(cols.get("SOURCE").toString()));
    }

    // get orgs to display in select
    List<Option> orgOptionList = EnrichmentPluginUtil
        .getOrgOptionList(getAnswerValue(), getWdkModel());
    
    return new FormViewModel(orgOptionList, sources);
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

    private List<Option> _orgOptions;
    private List<Option> _sourceOptions;

    public FormViewModel(List<Option> orgOptions, List<Option> sourceOptions) {
      _orgOptions = orgOptions;
      _sourceOptions = sourceOptions;
    }

    public List<Option> getOrganismOptions() {
      return _orgOptions;
    }

    public List<Option> getSourceOptions() {
      return _sourceOptions;
    }

    public String getOrganismParamHelp() { return EnrichmentPluginUtil.ORGANISM_PARAM_HELP; }
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
    public String getPvalueCutoff() { return EnrichmentPluginUtil.getPvalueCutoff(_formParams); }
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
