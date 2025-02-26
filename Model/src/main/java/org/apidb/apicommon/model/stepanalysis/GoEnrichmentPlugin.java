package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SingleLongResultSetHandler;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(GoEnrichmentPlugin.class);

  private static final String GO_TERM_BASE_URL_PROP_KEY = "goTermPageUrl";
  //private static final String GENE_SEARCH_BASE_URL_PROP_KEY = "geneSearchUrl";
  private static final String GO_EVID_CODE_PARAM_KEY = "goEvidenceCodes";
  //private static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";
  private static final String GO_ASSOC_ONTOLOGY_PARAM_KEY = "goAssociationsOntologies";
  private static final String GO_SUBSET_PARAM_KEY = "goSubset";

  private static final String TABBED_RESULT_FILE_PATH = "goEnrichmentResult.tsv";
  private static final String HIDDEN_TABBED_RESULT_FILE_PATH = "hiddenGoEnrichmentResult.tsv";
  private static final String IMAGE_RESULT_FILE_PATH = "goCloud.png";
  //we would create another one here for the word cloud file

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

  public ValidationBundle validateFormParams(Map<String, String> formParams) throws WdkModelException {

    ValidationBundleBuilder errors = ValidationBundle.builder(ValidationLevel.SEMANTIC);

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
    if (!errors.hasErrors()) {
        validateFilteredGoTerms(/*sourcesStr,*/  evidCodesStr, ontology, goSubset, errors);
    }

    return errors.build();
  }

  private void validateFilteredGoTerms(/*String sourcesStr,*/ String evidCodesStr, String ontology, String goSubset, ValidationBundleBuilder errors)
      throws WdkModelException {

    String idSql =  EnrichmentPluginUtil.getOrgSpecificIdSql(getAnswerValue(), getFormParams());
    String sql =
        "SELECT count(distinct gts.go_term_id) as " + NL +
        "  FROM ApidbTuning.GoTermSummary gts,"  + NL +
        "  (" + idSql + ") r"  + NL +
        "  where gts.gene_source_id = r.source_id" + NL +
        "    and gts.ontology = '" + ontology + "'" + NL +
        "    AND gts.evidence_category in (" + evidCodesStr + ")" + NL +
        "  and ("+ goSubset +" = 'No' or gts.is_go_slim = '1')" + NL ;

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    long result = new SQLRunner(ds, sql, "count-filtered-go-terms")
        .executeQuery(new SingleLongResultSetHandler())
        .orElseThrow(() -> new WdkModelException("No result found in count query: " + sql));

    if (result < 1) {
      errors.addError("Your result has no genes with GO Terms that satisfy the parameter choices you have made.  Please try adjusting the parameters.");
    }
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getWdkModel();
    Map<String,String> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);

    String evidCodesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_EVID_CODE_PARAM_KEY, params, null); // in sql format
    String ontology = EnrichmentPluginUtil.getSingleAllowableValueParam(
        GO_ASSOC_ONTOLOGY_PARAM_KEY, params, null); // only get first (and only) value
    String goSubset = EnrichmentPluginUtil.getArrayParamValueAsString(
        GO_SUBSET_PARAM_KEY, params, null); // in sql format

    // create another path here for the image word cloud JP LOOK HERE name it like imageFilePath
    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    Path hiddenResultFilePath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    Path imageResultFilePath = Paths.get(getStorageDirectory().toString(), IMAGE_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiGoEnrichment").toString();

    LOG.info(qualifiedExe +
        " " + resultFilePath.toString() +
        " " + idSql +
        " " + wdkModel.getProjectId() +
        " " + pValueCutoff +
        " " + ontology +
        " " + evidCodesStr +
        " " + goSubset +
        " " + imageResultFilePath.toString() +
        " " + hiddenResultFilePath.toString());

    // Catch exception when the  *chosen* organism has no GO Terms hits
    //File file = new File(hiddenResultFilePath.toString());
    //boolean existFile = file.exists();    
    //if (!existFile){
    //  errors.addError("Your result has no genes with GO Terms for this Organism. Please try changing the Organism parameter.");
    //  throw new  IllegalAnswerValueException("Your result has no genes with GO Terms for this Organism. Please try changing the Organism parameter.");
    //}

    return new String[] {
        qualifiedExe,
        resultFilePath.toString(),
        idSql,
        wdkModel.getProjectId(),
        pValueCutoff,
        ontology,
        evidCodesStr,
        goSubset,
        imageResultFilePath.toString(),
        hiddenResultFilePath.toString()
    };
  }

  /**
   * Make sure only one organism is represented in the results of this step
   *
   * @param answerValue answerValue that will be passed to this step
   */
  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {

    String idSql = answerValue.getIdSql();
    DataSource ds = getWdkModel().getAppDb().getDataSource();

    // check for non-zero count of genes with GO associations (ontology must be non-null)
    String sql = "select count(distinct gts.gene_source_id)" + NL +
      " from apidbtuning.GoTermSummary gts, (" + idSql + ") r" + NL +
      " where gts.gene_source_id = r.gene_source_id" + NL +
      " and gts.ontology is not null";

    LOG.info("Executing the following SQL: " + sql);

    long count = new SQLRunner(ds, sql, "count-go-genes").executeQuery(new SingleLongResultSetHandler())
        .orElseThrow(() -> new WdkModelException("No result found in count query: " + sql));

    LOG.info("Returned " + count);

    if (count == 0) {
      throw new IllegalAnswerValueException(
          "Your result has no genes with GO terms, " +
          "so you can't use this tool on this result. " +
          "Please revise your search and try again.");
    }
  }

  private ResultViewModel createResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    //    Path inputPath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    //    Path imageResultFilePath = Paths.get(getStorageDirectory().toString(), IMAGE_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (BufferedReader buffer = new BufferedReader(new FileReader(inputPath.toFile()))) {
      String line = buffer.readLine();  // throw away header line
      StringBuilder revigoInputLists = new StringBuilder();
      while ((line = buffer.readLine()) != null) {
        String[] columns = line.split(TAB);
        String revigo = columns[0] + " " + columns[8] + "\n";
        String val = EnrichmentPluginUtil.formatSearchLinkHtml(columns[4], columns[3]);
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

  @Override
  public JSONObject getResultViewModelJson() throws WdkModelException {
      return createResultViewModel().toJson();
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String> _formParams;
    private String _goTermBaseUrl;
    private String _imageDownloadPath;
    private String _hiddenDownloadPath;
    private String _revigoInputList;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String> formParams, String goTermBaseUrl, String imageDownloadPath,
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
    public String getGoTermBaseUrl() { return _goTermBaseUrl; }
    public String getRevigoInputList() {return _revigoInputList; }

    JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("headerRow", getHeaderRow().toJson());
      json.put("headerDescription", getHeaderDescription().toJson());
      JSONArray resultsJson = new JSONArray();
      for (ResultRow rr : getResultData()) resultsJson.put(rr.toJson());
      json.put("resultData", resultsJson);
      json.put("downloadPath", getDownloadPath());
      json.put("imageDownloadPath", getImageDownloadPath());
      json.put("hiddenDownloadPath", gethiddenDownloadPath());
      json.put("pvalueCutoff", getPvalueCutoff());
      json.put("evidenceCodes", new JSONArray(_formParams.get(GoEnrichmentPlugin.GO_EVID_CODE_PARAM_KEY)));
      json.put("goOntologies", new JSONArray(_formParams.get(GoEnrichmentPlugin.GO_ASSOC_ONTOLOGY_PARAM_KEY)));
      json.put("goSubset", new JSONArray(_formParams.get(GoEnrichmentPlugin.GO_SUBSET_PARAM_KEY)));
      json.put("goTermBaseUrl", getGoTermBaseUrl());
      json.put("revidoInputList", getRevigoInputList());
      return json;
    }
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

    public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("goId", _goId);
      json.put("goTerm", _goTerm);
      json.put("bgdGenes", _bgdGenes);
      json.put("resultGenes", _resultGenes);
      json.put("percentInResult", _percentInResult);
      json.put("foldEnrich", _foldEnrich);
      json.put("oddsRatio", _oddsRatio);
      json.put("pValue", _pValue);
      json.put("benjamini", _benjamini);
      json.put("bonferroni", _bonferroni);
      return json;
    }
  }
}
