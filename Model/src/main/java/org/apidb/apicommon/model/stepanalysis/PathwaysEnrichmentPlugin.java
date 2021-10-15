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
import java.sql.Types;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
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

public class PathwaysEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(PathwaysEnrichmentPlugin.class);

  private static final String PATHWAY_BASE_URL_PROP_KEY = "pathwayPageUrl";
  private static final String PATHWAYS_SRC_PARAM_KEY = "pathwaysSources";
  private static final String EXACT_MATCH_PARAM_KEY = "exact_match_only";
  private static final String EXCLUDE_INCOMPLETE_PARAM_KEY = "exclude_incomplete_ec";


  private static final String TABBED_RESULT_FILE_PATH = "pathwaysEnrichmentResult.tsv";
  private static final String HIDDEN_TABBED_RESULT_FILE_PATH = "hiddenPathwaysEnrichmentResult.tsv";
  private static final String IMAGE_RESULT_FILE_PATH = "goCloud.png";

  private static final ResultRow HEADER_ROW = new ResultRow(
      "Pathway ID", "Pathway Name", "Pathway Source", "Genes in the bkgd with this pathway","Genes in your result with this pathway", "Percent of bkgd Genes in your result", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni");

  private static final ResultRow COLUMN_HELP = new ResultRow(
      "Pathway ID",
      "Pathway Name",
      "Pathway Source",
      "Number of genes in this pathway in the background",
      "Number of genes in this pathway in your result",
      "Percentage of genes in the background in this pathway that are present in your result",
      "The percent of genes in this pathway in your result divided by the percent of genes in this pathway in the background",
      "Odds ratio statistic from the Fisher's exact test",
      "P-value from Fisher's exact test",
      "Benjamini-Hochberg false discovery rate (FDR)",
      "Bonferroni adjusted p-value"
  );

  // TODO: verify that validation is being performed here (i.e. that these params live in the model
  @SuppressWarnings("unused")
  private ValidationBundle validateFormParamValues(Map<String, String> formParams) throws WdkModelException {

    ValidationBundleBuilder errors = ValidationBundle.builder(ValidationLevel.SEMANTIC);

    // validate pValueCutoff
    EnrichmentPluginUtil.validatePValue(formParams, errors);

    // validate organism
    EnrichmentPluginUtil.validateOrganism(formParams, getAnswerValue(), getWdkModel(), errors);

    // validate annotation sources
    EnrichmentPluginUtil.getArrayParamValueAsString(PATHWAYS_SRC_PARAM_KEY, formParams, errors);

    // only validate further if the above pass
    if (!errors.hasErrors()) {
      validateFilteredPathways(errors);
    }

    return errors.build();
  }

  private void validateFilteredPathways(ValidationBundleBuilder errors)
        throws WdkModelException {
    String countColumn = "CNT";

    Map<String, String> formParams = getFormParams();
    if (!formParams.containsKey(EXCLUDE_INCOMPLETE_PARAM_KEY)) {
      errors.addError(EXCLUDE_INCOMPLETE_PARAM_KEY, "Missing required parameter.");
    }
    if (!formParams.containsKey(EXACT_MATCH_PARAM_KEY)) {
      errors.addError(EXACT_MATCH_PARAM_KEY, "Missing required parameter.");
    }

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(getAnswerValue(), formParams);
    String sql =
        "SELECT count (distinct tp.pathway_source_id) as " + countColumn + NL +
        "FROM   apidbtuning.transcriptPathway tp, " + NL +
        "(" + idSql + ") r" + NL +
        "WHERE  tp.gene_source_id = r.source_id" + NL +
        "AND tp.complete_ec >= ?" + NL +
        "AND tp.exact_match >= ?";

    LOG.info(sql);
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql, "count-filtered-pathways").executeQuery(
      new Object[]{ formParams.get(EXCLUDE_INCOMPLETE_PARAM_KEY), formParams.get(EXACT_MATCH_PARAM_KEY) },
      new Integer[]{ Types.BINARY, Types.BINARY },
      handler
    );

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);

    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() > 1) {
      errors.addError("Your result has no genes with Pathways that satisfy the parameter choices you have made.  Please try adjusting the parameters.");
    }
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getAnswerSpec().getQuestion().getWdkModel();
    Map<String,String> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);
    String sourcesStr = EnrichmentPluginUtil.getArrayParamValueAsString(
        PATHWAYS_SRC_PARAM_KEY, params, null); // in sql format

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    Path hiddenResultFilePath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    Path imageResultFilePath = Paths.get(getStorageDirectory().toString(), IMAGE_RESULT_FILE_PATH);

    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiPathwaysEnrichment").toString();
    LOG.info(qualifiedExe + " " + resultFilePath.toString() + " " + idSql + " " +
        wdkModel.getProjectId() + " " + pValueCutoff + imageResultFilePath.toString() + hiddenResultFilePath.toString());
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql, wdkModel.getProjectId(), pValueCutoff,
			 sourcesStr, imageResultFilePath.toString(), hiddenResultFilePath.toString()};
  }

  /**
   * Make sure only one organism is represented in the results of this step
   *
   * @param answerValue answerValue that will be passed to this step
   */
  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {

    String countColumn = "CNT";
    String idSql = answerValue.getIdSql();
    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();

    // check for non-zero count of genes with Pathways
    String sql = "SELECT count (distinct gp.gene_source_id) as " + countColumn + NL +
      "from  apidbtuning.transcriptPathway gp, (" + idSql + ") r" + NL +
      "WHERE  gp.gene_source_id = r.gene_source_id";

    new SQLRunner(ds, sql, "count-pathway-genes").executeQuery(handler);

    if (handler.getNumRows() == 0) throw new WdkModelException("No result found in count query: " + sql);

    Map<String, Object> result = handler.getResults().get(0);
    BigDecimal count = (BigDecimal)result.get(countColumn);

    if (count.intValue() == 0 ) {
      throw new IllegalAnswerValueException(
          "Your result has no genes that are in pathways, " +
          "so you can't use this tool on this result. " +
          "Please revise your search and try again.");
    }
  }

  @Override
  public JSONObject getResultViewModelJson() throws WdkModelException {
    return createResultViewModel().toJson();
  }

  private ResultViewModel createResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), HIDDEN_TABBED_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (BufferedReader buffer = new BufferedReader(new FileReader(inputPath.toFile()))) {
      String line = buffer.readLine();  // throw away header line
      while ((line = buffer.readLine()) != null) {
        String[] columns = line.split(TAB);
        String[] primaryKeys = columns[0].split("__PK__");  // source_id and pathway_source (eg, KEGG)
        String val = EnrichmentPluginUtil.formatSearchLinkHtml(columns[4], columns[3]);
        if (primaryKeys.length != 2) throw new WdkModelException ("invalid compbined primary key: " + columns[0]);
        results.add(new ResultRow(primaryKeys[0], columns[1], primaryKeys[1], columns[2], val, columns[5], columns[6], columns[7], columns[8], columns[9], columns[10]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams(), getProperty(PATHWAY_BASE_URL_PROP_KEY), IMAGE_RESULT_FILE_PATH, HIDDEN_TABBED_RESULT_FILE_PATH);
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String> _formParams;
    private String _pathwayBaseUrl;
      private String _imageDownloadPath;
      private String _hiddenDownloadPath;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
			   Map<String, String> formParams, String pathwayBaseUrl, String imageDownloadPath, String hiddenDownloadPath) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = resultData;
      _pathwayBaseUrl = pathwayBaseUrl;
      this._imageDownloadPath = imageDownloadPath;
      this._hiddenDownloadPath = hiddenDownloadPath;

    }

    public ResultRow getHeaderRow() { return PathwaysEnrichmentPlugin.HEADER_ROW; }
    public ResultRow getHeaderDescription() { return PathwaysEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getImageDownloadPath() { return _imageDownloadPath; }
    public String gethiddenDownloadPath() { return _hiddenDownloadPath; }
    public String getPvalueCutoff() { return EnrichmentPluginUtil.getPvalueCutoff(_formParams); }
    public String getPathwayBaseUrl() { return _pathwayBaseUrl; }

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
      json.put("pathwaySources", new JSONArray(_formParams.get(PathwaysEnrichmentPlugin.PATHWAYS_SRC_PARAM_KEY)));
      json.put("pathwayBaseUrl", getPathwayBaseUrl());
      return json;
    }

  }

  public static class ResultRow {

    private String _pathwayId;
    private String _pathwayName;
    private String _pathwaySource;
    private String _bgdGenes;
    private String _resultGenes;
    private String _percentInResult;
    private String _foldEnrich;
    private String _oddsRatio;
    private String _pValue;
    private String _benjamini;
    private String _bonferroni;

    public ResultRow(String pathwayId, String pathwayName, String pathwaySource, String bgdGenes, String resultGenes, String percentInResult, String foldEnrich, String oddsRatio, String pValue, String benjamini, String bonferroni) {
      _pathwayId = pathwayId;
      _pathwayName = pathwayName;
      _pathwaySource = pathwaySource;
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
    public String getPathwaySource() { return _pathwaySource; }
    public String getBgdGenes() { return _bgdGenes; }
    public String getResultGenes() { return _resultGenes; }
    public String getPercentInResult() { return _percentInResult; }
    public String getFoldEnrich() { return _foldEnrich; }
    public String getOddsRatio() { return _oddsRatio; }
    public String getPvalue() { return _pValue; }
    public String getBenjamini() { return _benjamini; }
    public String getBonferroni() { return _bonferroni; }

    public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("pathwayId", _pathwayId);
      json.put("pathwayName", _pathwayName);
      json.put("pathwaySource", _pathwaySource);
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
