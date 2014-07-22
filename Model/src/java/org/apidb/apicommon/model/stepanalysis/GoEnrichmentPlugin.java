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

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);

  private static final String GO_TERM_BASE_URL_PROP_KEY = "goTermPageUrl";
  
  private static final String PVALUE_PARAM_KEY = "pValueCutoff";
  //  public static final String GO_EVID_CODE_PARAM_KEY = "goEvidenceCodes";
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
    validatePValue(formParams, errors);

    // validate annotation sources 
    String sourcesStr = getArrayParamValueAsString(GO_ASSOC_SRC_PARAM_KEY, formParams, errors);

    /*
    // validate evidence codes
        String evidCodesStr = getArrayParamValueAsString(GO_EVID_CODE_PARAM_KEY, formParams, errors);
    */

    // validate ontology
    String ontology = getOntologyParamValue(formParams, errors);

    // only validate further if the above pass
    if (errors.isEmpty()) {
      validateFilteredGoTerms(sourcesStr, /*evidCodesStr,*/ ontology, errors);
    }

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
        errors.addParamMessage(PVALUE_PARAM_KEY, "Must be a number greater than 0 and less than or equal to 1.");
      }
    }
  }

  // return Ontology param value
  // @param errors may be null if the sources have been previously validated.
  private String getOntologyParamValue(Map<String, String[]> formParams, ValidationErrors errors) {
    String[] ontologies = formParams.get(GO_ASSOC_ONTOLOGY_PARAM_KEY);
    if ((ontologies == null || ontologies.length != 1) && errors != null) {
      errors.addParamMessage(GO_ASSOC_ONTOLOGY_PARAM_KEY, "Missing required parameter, or more than one provided.");
      return null;
    }
    return ontologies[0];
  }

  private void validateFilteredGoTerms(String sourcesStr,/* String evidCodesStr,*/ String ontology, ValidationErrors errors) throws WdkModelException, WdkUserException {

    String countColumn = "CNT";
    String idSql = getAnswerValue().getIdSql();
    String sql = "SELECT count(distinct gts.go_term_id) as " + countColumn + NL +
      "FROM ApidbTuning.GoTermSummary gts,"  + NL +
      "(" + idSql + ") r"  + NL +
      "where gts.source_id = r.source_id" + NL +
      "and gts.ontology = '" + ontology + "'" + NL +
      "and gts.source in (" + sourcesStr + ")" + NL
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
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get(PVALUE_PARAM_KEY)[0];
    String sourcesStr = getArrayParamValueAsString(GO_ASSOC_SRC_PARAM_KEY, params, null); // in sql format
    // String evidCodesStr = getArrayParamValueAsString(GO_EVID_CODE_PARAM_KEY, params, null); // in sql format
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
    String sql = "select count(distinct gts.source_id) as " + countColumn + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id";

    new SQLRunner(ds, sql).executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);
    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() == 0 ) {
      throw new IllegalAnswerValueException("Your result has no genes with GO terms, so you can't use this tool on this result. " +
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
          "one organism.  The GO Enrichment analysis only accepts gene " +
          "lists from one organism.  Please use the Filter boxes to limit your " +
          "result to a single organism and try again.");
    }
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException, WdkUserException {
    
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    String idSql = getAnswerValue().getIdSql();
    
    // find annotation sources used in the result set
    String sql = "select distinct gts.source" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> sources = new ArrayList<>();
    int rowCnt = 0;
    String sourcesStr = "";

    // HACK: For now, allow only two sources, interpro and the annotation center.  We do this so we can hard-code
    // reasonable display names for each (which are not available in the database).  If we ever have more than just
    // these two sources this hack will not work and we'll need to find a way to do it in the database
    // DO NOT change this hack without making a parallel change in the perl code
    for (Map<String,Object> cols : handler.getResults()) {
      String src = cols.get("SOURCE").toString();
      String srcDisplay = src.toLowerCase().equals("interpro")? "InterPro predictions" : "Annotation Center";
      rowCnt++;
      sources.add(new Option(src, srcDisplay));
    }
    if (rowCnt > 2) throw new WdkModelException("Found more than two sources for GO Annotation: " + sourcesStr);

    // find ontologies used in the result set
    sql = "select distinct gts.ontology" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id and gts.ontology is not null";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> ontologies = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      ontologies.add(new Option(cols.get("ONTOLOGY").toString()));
    }

    /*
    // find evidence codes used in the result set
    sql = "select distinct gts.evidence_code" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.source_id = r.source_id";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<String> evidCodes = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      evidCodes.add(cols.get("EVIDENCE_CODE").toString());
    }
    */
    return new FormViewModel(sources, ontologies /*, evidCodes*/, getWdkModel().getProjectId());
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

  public static class Option {
    private String _term;
    private String _display;
    public Option(String term) { this(term, term); }
    public Option(String term, String display) {
      _term = term; _display = display;
    }
    public String getTerm() { return _term; }
    public String getDisplay() { return _display; }
  }
  
  public static class FormViewModel {
    
    private List<Option> _sourceOptions;
    private List<Option> _ontologyOptions;
    // private List<String> _evidCodeOptions;
    private String _projectId;
    
    public FormViewModel(List<Option> sourceOptions, List<Option> ontologyOptions /*, List<String> evidCodeOptions*/, String projectId) {
      _sourceOptions = sourceOptions;
      _ontologyOptions = ontologyOptions;
      // _evidCodeOptions = evidCodeOptions;
      _projectId = projectId;
    }

    public List<Option> getSourceOptions() {
      return _sourceOptions;
    }

    /*
    public List<String> getEvidCodeOptions() {
      return _evidCodeOptions;
    }
    */

    public List<Option> getOntologyOptions() {
      return _ontologyOptions;
    }
    
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
    public String getPvalueCutoff() { return _formParams.get(GoEnrichmentPlugin.PVALUE_PARAM_KEY)[0]; }
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
