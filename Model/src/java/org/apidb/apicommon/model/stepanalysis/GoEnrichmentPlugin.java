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

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);

  private static final String GO_TERM_BASE_URL_PROP_KEY = "goTermPageUrl";
  // private static final String GO_EVID_CODE_PARAM_KEY = "goEvidenceCodes";
  private static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";
  private static final String GO_ASSOC_ONTOLOGY_PARAM_KEY = "goAssociationsOntologies";

  private static final String TABBED_RESULT_FILE_PATH = "goEnrichmentResult.tab";

  private static final String ONTOLOGY_PARAM_HELP =
      "<p>Choose the Ontology that you are interested in analyzing. Only terms " +
      "from this ontology will be considered during the enrichment analysis.</p>" +
      "<p>The ontologies are three structured, controlled vocabularies that describe " +
      "gene products in terms of their related biological processes, cellular " +
      "components and molecular functions. For statistical reasons, only one " +
      "ontology may be analyzed at once. If you are interested in more than one, " +
      "run separate GO enrichment analyses.</p>";

  private static final String PROJECT_ID_KEY = "@PROJECT_ID@";
  private static final String SOURCES_PARAM_HELP =
      "<p>Choose the GO Association Source(s) that you wish to include in the analysis.</p>" + 
      "<ol style='list-style:inside'>GO terms in " + 
      PROJECT_ID_KEY +  " are associated with genes by either:" + 
      "<li>mapping gene products to the InterPro domain database resulting in 100% " + 
      "electronically transferred GO associations.</li>" + 
      "<li>downloading associations from the sequencing centers (e.g. GeneDB or JCVI)" + 
      " which may include a combination of electronically transferred or manually curated associations.</li>" + 
      "</ul>" +
      "<p>Not all sources are available for every genome.</p>";

  private static final String PVALUE_PARAM_HELP =
      "<p>Choose the P-Value Cutoff that a GO term must meet before it is " +
      "considered enriched in your gene result. The P-value is a statistical " +
      "measure of the likelihood that a certain GO term appears among the " +
      "genes in your results more often than it appears in the set of all " +
      "genes for that organism (background).</p>";

  public static final ResultRow HEADER_ROW = new ResultRow(
      "GO ID", "GO Term", "Genes in the bkgd with this term", "Genes in your result with this term", "Percent of bkgd Genes in your result", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni");

  public static final ResultRow COLUMN_HELP = new ResultRow(
      "Gene Ontology ID",
      "Gene Ontology Term",
      "Number of genes with this term in the background",
      "Number of genes with this term in your result",
      "Of the genes in the background with this term, the percent that are present in your result",
      "The percent of genes with this term in your result divided by the percent of genes with this term in the background",
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
        GO_ASSOC_SRC_PARAM_KEY, formParams, errors);

    /*// validate evidence codes
    String evidCodesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_EVID_CODE_PARAM_KEY, formParams, errors); */

    // validate ontology
    String ontology = EnrichmentPluginUtil.getSingleAllowableValueParam(
        GO_ASSOC_ONTOLOGY_PARAM_KEY, formParams, errors);

    // only validate further if the above pass
    if (errors.isEmpty()) {
      validateFilteredGoTerms(sourcesStr, /*evidCodesStr,*/ ontology, errors);
    }

    return errors;
  }

  private void validateFilteredGoTerms(String sourcesStr,/* String evidCodesStr,*/ String ontology, ValidationErrors errors)
      throws WdkModelException, WdkUserException {

    String countColumn = "CNT";
    String idSql =  EnrichmentPluginUtil.getOrgSpecificIdSql(getAnswerValue(), getFormParams());
    String sql = "SELECT count(distinct gts.go_term_id) as " + countColumn + NL +
      "FROM ApidbTuning.GoTermSummary gts,"  + NL +
      "(" + idSql + ") r"  + NL +
      "where gts.gene_source_id = r.gene_source_id" + NL +
      "and gts.ontology = '" + ontology + "'" + NL +
      "and gts.displayable_source in (" + sourcesStr + ")" + NL
      // +  "and gts.evidence_code in (" + evidCodesStr + ")" + NL
      ;

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);

    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() < 1) {
      errors.addMessage("Your result has no genes with GO Terms that satisfy the parameter choices you have made.  Please try adjusting the parameters.");
    }
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    Map<String,String[]> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);
    String sourcesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_ASSOC_SRC_PARAM_KEY, params, null); // in sql format
    //String evidCodesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
    //    GO_EVID_CODE_PARAM_KEY, params, null); // in sql format
    String ontology = params.get(GO_ASSOC_ONTOLOGY_PARAM_KEY)[0];

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiGoEnrichment").toString();
    LOG.info(qualifiedExe + " " + resultFilePath.toString() + " " + idSql + " " + 
        wdkModel.getProjectId() + " " + pValueCutoff + " " + ontology + " " + sourcesStr);
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql,
        wdkModel.getProjectId(), pValueCutoff, ontology, sourcesStr, /* evidCodesStr */ };
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

    // check for non-zero count of genes with GO associations
    String sql = "select count(distinct gts.gene_source_id) as " + countColumn + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.gene_source_id = r.gene_source_id";

    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);
    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() == 0 ) {
      throw new IllegalAnswerValueException(
          "Your result has no genes with GO terms, " +
          "so you can't use this tool on this result. " +
          "Please revise your search and try again.");
    }
  }

  @Override
  public Object getFormViewModel() throws WdkModelException, WdkUserException {

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    String idSql = getAnswerValue().getIdSql();

    // find annotation sources used in the result set
    String sql = "select distinct gts.displayable_source" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.gene_source_id = r.gene_source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> sources = new ArrayList<>();

    for (Map<String,Object> cols : handler.getResults()) {
      String srcDisplay = cols.get("DISPLAYABLE_SOURCE").toString();
      sources.add(new Option(srcDisplay, srcDisplay));
    }

    // find ontologies used in the result set
    sql = "select distinct gts.ontology" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.gene_source_id = r.gene_source_id and gts.ontology is not null";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> ontologies = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      ontologies.add(new Option(cols.get("ONTOLOGY").toString()));
    }

    /*
    // find evidence codes used in the result set
    sql = "select distinct gts.evidence_code" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.gene_source_id = r.gene_source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> evidCodes = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      evidCodes.add(cols.get("EVIDENCE_CODE").toString());
    }
    */

    // get orgs to display in select
    List<Option> orgOptionList = EnrichmentPluginUtil
        .getOrgOptionList(getAnswerValue(), getWdkModel());

    return new FormViewModel(orgOptionList, sources, ontologies /*, evidCodes*/, getWdkModel().getProjectId());
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
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams(), getProperty(GO_TERM_BASE_URL_PROP_KEY));
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {

    private List<Option> _orgOptions;
    private List<Option> _sourceOptions;
    private List<Option> _ontologyOptions;
    // private List<String> _evidCodeOptions;
    private String _projectId;

    public FormViewModel(List<Option> orgOptions, List<Option> sourceOptions, List<Option> ontologyOptions /*, List<String> evidCodeOptions*/, String projectId) {
      _orgOptions = orgOptions;
      _sourceOptions = sourceOptions;
      _ontologyOptions = ontologyOptions;
      // _evidCodeOptions = evidCodeOptions;
      _projectId = projectId;
    }

    public List<Option> getOrganismOptions() {
      return _orgOptions;
    }

    public List<Option> getSourceOptions() {
      return _sourceOptions;
    }

    /* public List<String> getEvidCodeOptions() {
      return _evidCodeOptions;
    } */

    public List<Option> getOntologyOptions() {
      return _ontologyOptions;
    }

    public String getOrganismParamHelp() { return EnrichmentPluginUtil.ORGANISM_PARAM_HELP; }
    public String getOntologyParamHelp() { return ONTOLOGY_PARAM_HELP; }
    public String getSourcesParamHelp() { return SOURCES_PARAM_HELP.replace(PROJECT_ID_KEY, _projectId); }
    public String getPvalueParamHelp() { return PVALUE_PARAM_HELP; }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    private String _goTermBaseUrl;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String[]> formParams, String goTermBaseUrl) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = resultData;
      _goTermBaseUrl = goTermBaseUrl;
    }

    public ResultRow getHeaderRow() { return GoEnrichmentPlugin.HEADER_ROW; }
    public ResultRow getHeaderDescription() { return GoEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return EnrichmentPluginUtil.getPvalueCutoff(_formParams); }
    public String getGoSources() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_SRC_PARAM_KEY), ", "); }
    // public String getEvidCodes() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_EVID_CODE_PARAM_KEY), ", "); }
    public String getGoOntologies() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_ONTOLOGY_PARAM_KEY), ", "); }
    public String getGoTermBaseUrl() { return _goTermBaseUrl; }
  }

  public static class ResultRow {

    private String _goId;
    private String _goTerm;
    private String _bgdGenes;
    private String _resultGenes;
    private String _percentInResult;
    private String _foldEnrich;
    private String _oddsRatio;
    private String _pValue;
    private String _benjamini;
    private String _bonferroni;

    public ResultRow(String goId, String goTerm, String bgdGenes, String resultGenes, String percentInResult, String foldEnrich, String oddsRatio, String pValue, String benjamini, String bonferroni) {
      _goId = goId;
      _goTerm = goTerm;
      _bgdGenes = bgdGenes;
      _resultGenes = resultGenes;
      _percentInResult = percentInResult;
      _foldEnrich = foldEnrich;
      _oddsRatio = oddsRatio;
      _pValue = pValue;
      _benjamini = benjamini;
      _bonferroni = bonferroni;
    }

    public String getGoId() { return _goId; }
    public String getGoTerm() { return _goTerm; }
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
