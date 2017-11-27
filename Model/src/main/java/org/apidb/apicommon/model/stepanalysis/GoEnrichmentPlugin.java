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
import org.gusdb.fgputil.db.runner.SingleLongResultSetHandler;
import org.gusdb.fgputil.db.runner.SingleLongResultSetHandler.Status;
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
    //  private static final String GENE_SEARCH_BASE_URL_PROP_KEY = "geneSearchUrl";
  private static final String GO_EVID_CODE_PARAM_KEY = "goEvidenceCodes";
    //private static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";
  private static final String GO_ASSOC_ONTOLOGY_PARAM_KEY = "goAssociationsOntologies";
  private static final String GO_SUBSET_PARAM_KEY = "goSubset";

  private static final String TABBED_RESULT_FILE_PATH = "goEnrichmentResult.tab";
  private static final String HIDDEN_TABBED_RESULT_FILE_PATH = "hiddenGoEnrichmentResult.tab";
  private static final String IMAGE_RESULT_FILE_PATH = "goCloud.png";
    //we would create another one here for the word cloud file 

  private static final String ONTOLOGY_PARAM_HELP =
      "<p>Choose the Ontology that you are interested in analyzing. Only terms " +
      "from this ontology will be considered during the enrichment analysis.</p>" +
      "<p>The ontologies are three structured, controlled vocabularies that describe " +
      "gene products in terms of their related biological processes, cellular " +
      "components and molecular functions. For statistical reasons, only one " +
      "ontology may be analyzed at once. If you are interested in more than one, " +
      "run separate GO enrichment analyses.</p>";

  private static final String EVIDENCE_PARAM_HELP =
      "<p>A GO Evidence Code of IEA is assigned to a computationally assigned association." + 
      "All others have some degree of curation</p>";

  private static final String PVALUE_PARAM_HELP =
      "<p>Choose the P-Value Cutoff that a GO term must meet before it is " +
      "considered enriched in your gene result. The P-value is a statistical " +
      "measure of the likelihood that a certain GO term appears among the " +
      "genes in your results more often than it appears in the set of all " +
      "genes for that organism (background).</p>";

 private static final String GO_SUBSET_PARAM_HELP =
     "<p> Choose Yes to limit enrichment analysis " +
     "based on terms that are in the GO Slim generic subset. " +
     "This will limit both the background and the gene list of interest.</p>";

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
    //    String sourcesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
    //        GO_ASSOC_SRC_PARAM_KEY, formParams, errors);

    // validate evidence codes
    String evidCodesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_EVID_CODE_PARAM_KEY, formParams, errors); 

    // validate ontology
    String ontology = EnrichmentPluginUtil.getSingleAllowableValueParam(
        GO_ASSOC_ONTOLOGY_PARAM_KEY, formParams, errors);

    //validate GOSubset  
    String goSubset = EnrichmentPluginUtil.getArrayParamValueAsString(
    	GO_SUBSET_PARAM_KEY, formParams, errors);

    // only validate further if the above pass
    if (errors.isEmpty()) {
        validateFilteredGoTerms(/*sourcesStr,*/  evidCodesStr, ontology, goSubset, errors);
    }

    return errors;
  }

    private void validateFilteredGoTerms(/*String sourcesStr,*/ String evidCodesStr, String ontology, String goSubset, ValidationErrors errors)
      throws WdkModelException, WdkUserException {

    String idSql =  EnrichmentPluginUtil.getOrgSpecificIdSql(getAnswerValue(), getFormParams());
    String sql =
        "SELECT count(distinct gts.go_term_id) as " + NL +
        "  FROM ApidbTuning.GoTermSummary gts,"  + NL +
        "  (" + idSql + ") r"  + NL +
        "  where gts.gene_source_id = r.source_id" + NL +
        "    and gts.ontology = '" + ontology + "'" + NL +
        "    AND decode(gts.evidence_code, 'IEA', 'Computed', 'Curated') in (" + evidCodesStr + ")" + NL +
	" and case when "+ goSubset +" = 'Yes' and gts.is_go_slim = '1' then 1" + NL +
        "     when (" + goSubset + " = 'No' and (gts.is_go_slim = '1' or gts.is_go_slim = '0')) then 1" + NL +
        "     else 0" + NL +
        "     end = 1" + NL ;


    DataSource ds = getWdkModel().getAppDb().getDataSource();
    SingleLongResultSetHandler result =
        new SQLRunner(ds, sql, "count-filtered-go-terms")
          .executeQuery(new SingleLongResultSetHandler());

    if (!Status.NON_NULL_VALUE.equals(result.getStatus())) {
      throw new WdkModelException("No result found in count query: " + sql);
    }

    if (result.getRetrievedValue() < 1) {
      errors.addMessage("Your result has no genes with GO Terms that satisfy the parameter choices you have made.  Please try adjusting the parameters.");
    }
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    Map<String,String[]> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);

    String evidCodesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_EVID_CODE_PARAM_KEY, params, null); // in sql format
    String ontology = params.get(GO_ASSOC_ONTOLOGY_PARAM_KEY)[0];
    String goSubset = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_SUBSET_PARAM_KEY, params, null); // in sql format
    // create another path here for the image word cloud JP LOOK HERE name it like imageFilePath
    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    Path hiddenResultFilePath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    Path imageResultFilePath = Paths.get(getStorageDirectory().toString(), IMAGE_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiGoEnrichment").toString();
    LOG.info(qualifiedExe + " " + resultFilePath.toString() + " " + idSql + " " + 
	     wdkModel.getProjectId() + " " + pValueCutoff + " " + ontology + " " + evidCodesStr + " " + goSubset + " " + imageResultFilePath.toString() + hiddenResultFilePath.toString());
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql,
                         wdkModel.getProjectId(), pValueCutoff, ontology, /*sourcesStr */ evidCodesStr, goSubset,  imageResultFilePath.toString(), hiddenResultFilePath.toString() };
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

    new SQLRunner(ds, sql, "count-go-genes").executeQuery(handler);

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

      // JP I THINK I NEED TO ADD SOMETHING HERE 

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    String idSql = getAnswerValue().getIdSql();

    // find ontologies used in the result set
    String sql = "select distinct gts.ontology" + NL +
      "from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      "where gts.gene_source_id = r.gene_source_id and gts.ontology is not null";
    new SQLRunner(ds, sql, "select-go-term-ontologies").executeQuery(handler);
    List<Option> ontologies = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
      ontologies.add(new Option(cols.get("ONTOLOGY").toString()));
    }

    // find evidence codes used in the result set
    sql = "select 'Curated' as evidence from dual union select 'Computed' from dual";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> evidCodes = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
        String evidString = cols.get("EVIDENCE").toString();
        evidCodes.add(new Option(evidString, evidString));
    }

    // find goSubset used in the result set
    sql = "select 'Yes' as gosubset from dual union select 'No' from dual";
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Option> goSubsets = new ArrayList<>();
    for (Map<String,Object> cols : handler.getResults()) {
        String goSubset = cols.get("GOSUBSET").toString();
        goSubsets.add(new Option(goSubset, goSubset));
    }


    // get orgs to display in select
    List<Option> orgOptionList = EnrichmentPluginUtil
        .getOrgOptionList(getAnswerValue(), getWdkModel());

    return new FormViewModel(orgOptionList, /*sources,*/ ontologies , evidCodes , goSubsets);
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    //    Path inputPath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    //    Path imageResultFilePath = Paths.get(getStorageDirectory().toString(), IMAGE_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      if (buffer.ready()) buffer.readLine();  // throw away header line	
      StringBuilder revigoInputLists = new StringBuilder();
      while (buffer.ready()) {
        String line = buffer.readLine();
        String[] columns = line.split(TAB);
        String revigo = columns[0] + " " + columns[8] + "\n";
        String val = "<a href=\"/a/showQuestion.do?questionFullName=GeneQuestions.GeneByLocusTag&ds_gene_ids_data=" + columns[4] + "\">" + columns[3] + "</a>";
        results.add(new ResultRow(columns[0], columns[1], columns[2], val, columns[5], columns[6], columns[7], columns[8], columns[9], columns[10]));
        revigoInputLists.append(revigo);
     }
      String revigoInputList = String.valueOf(revigoInputLists);
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams(), getProperty(GO_TERM_BASE_URL_PROP_KEY), IMAGE_RESULT_FILE_PATH, HIDDEN_TABBED_RESULT_FILE_PATH, revigoInputList);
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {
    private List<Option> _orgOptions;
    private List<Option> _ontologyOptions;
    private List<Option> _evidCodeOptions;
    private List<Option> _goSubsetOptions;   
      public FormViewModel(List<Option> orgOptions, List<Option> ontologyOptions, List<Option> evidCodeOptions, List<Option> goSubsetOptions) {
      _orgOptions = orgOptions;
      _ontologyOptions = ontologyOptions;
      _evidCodeOptions = evidCodeOptions;
      _goSubsetOptions = goSubsetOptions;
    }

    public List<Option> getOrganismOptions() {
      return _orgOptions;
    }

    public List<Option> getEvidCodeOptions() {
      return _evidCodeOptions;
    } 

    public List<Option> getOntologyOptions() {
      return _ontologyOptions;
    }

    public List<Option> getGoSubsetOptions() {
      return _goSubsetOptions;
    }

    public String getOrganismParamHelp() { return EnrichmentPluginUtil.ORGANISM_PARAM_HELP; }
    public String getOntologyParamHelp() { return ONTOLOGY_PARAM_HELP; }
    public String getEvidenceParamHelp() { return EVIDENCE_PARAM_HELP; }
    public String getPvalueParamHelp() { return PVALUE_PARAM_HELP; }
    public String getGoSubsetParamHelp() { return GO_SUBSET_PARAM_HELP; }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String[]> _formParams;
    private String _goTermBaseUrl;
    private String _imageDownloadPath;
    private String _hiddenDownloadPath;
    private String _revigoInputList;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String[]> formParams, String goTermBaseUrl, String imageDownloadPath,
        String hiddenDownloadPath, String revigoInputList) {
      this._downloadPath = downloadPath;
      this._formParams = formParams;
      this._resultData = resultData;
      this._goTermBaseUrl = goTermBaseUrl;
      this._imageDownloadPath = imageDownloadPath;
      this._hiddenDownloadPath = hiddenDownloadPath;
      this._revigoInputList= revigoInputList;
    }

    public ResultRow getHeaderRow() { return GoEnrichmentPlugin.HEADER_ROW; }
    public ResultRow getHeaderDescription() { return GoEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getImageDownloadPath() { return _imageDownloadPath; }
    public String gethiddenDownloadPath() { return _hiddenDownloadPath; }
    public String getPvalueCutoff() { return EnrichmentPluginUtil.getPvalueCutoff(_formParams); }
    public String getEvidCodes() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_EVID_CODE_PARAM_KEY), ", "); }
    public String getGoOntologies() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_ONTOLOGY_PARAM_KEY), ", "); }
    public String getGoSubset() { return FormatUtil.join(_formParams.get(GoEnrichmentPlugin.GO_SUBSET_PARAM_KEY), ", "); }
    public String getGoTermBaseUrl() { return _goTermBaseUrl; }
    public String getRevigoInputList() {return _revigoInputList; }
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
