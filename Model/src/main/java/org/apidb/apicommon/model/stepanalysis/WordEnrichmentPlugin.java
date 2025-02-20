package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.TAB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.json.JSONArray;
import org.json.JSONObject;

public class WordEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(WordEnrichmentPlugin.class);

  public static final String TABBED_RESULT_FILE_PATH = "wordEnrichmentResult.tsv";

  public static final ResultRow HEADER_ROW = new ResultRow(
      "Word", "Description", "Genes in the bkgd with this word", "Genes in your result with this word", "Percent of bkgd Genes in your result", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni");

  public static final ResultRow COLUMN_HELP = new ResultRow(
      "Word",
      "Description",
      "Number of genes with this word in the background (the reference genome)",
      "Number of genes with this word in your result",
      "Of the genes in the background with this word, the percent that are present in your result",
      "The percent of genes with this word in your result divided by the percent of genes with this word in the background",
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

    return errors.build();
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getWdkModel();
    Map<String,String> params = getFormParams();

    String idSql = EnrichmentPluginUtil.getOrgSpecificIdSql(answerValue, params);
    String pValueCutoff = EnrichmentPluginUtil.getPvalueCutoff(params);

    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "apiWordEnrichment").toString();
    LOG.info(qualifiedExe + " " + resultFilePath.toString() + " " + idSql + " " +
        wdkModel.getProjectId() + " " + pValueCutoff);
    return new String[]{ qualifiedExe, resultFilePath.toString(), idSql, wdkModel.getProjectId(), pValueCutoff};
  }

  @Override
  public JSONObject getResultViewModelJson() throws WdkModelException {
    return createResultViewModel().toJson();
  }

  private ResultViewModel createResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);
    List<ResultRow> results = new ArrayList<>();
    try (BufferedReader buffer = new BufferedReader(new FileReader(inputPath.toFile()))) {
      String line = buffer.readLine();  // throw away header line
      while ((line = buffer.readLine()) != null) {
        String[] columns = line.split(TAB);
        results.add(new ResultRow(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5], columns[6], columns[7], columns[8], columns[9]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams());
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class ResultViewModel {

    private List<ResultRow> _resultData;
    private String _downloadPath;
    private Map<String, String> _formParams;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
        Map<String, String> formParams) {
      _downloadPath = downloadPath;
      _formParams = formParams;
      _resultData = resultData;
    }

    public ResultRow getHeaderRow() { return WordEnrichmentPlugin.HEADER_ROW; }
    public ResultRow getHeaderDescription() { return WordEnrichmentPlugin.COLUMN_HELP; }
    public List<ResultRow> getResultData() { return _resultData; }
    public String getDownloadPath() { return _downloadPath; }
    public String getPvalueCutoff() { return EnrichmentPluginUtil.getPvalueCutoff(_formParams); }

    JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("headerRow", getHeaderRow().toJson());
      json.put("headerDescription", getHeaderDescription().toJson());
      JSONArray resultsJson = new JSONArray();
      for (ResultRow rr : getResultData()) resultsJson.put(rr.toJson());
      json.put("resultData", resultsJson);
      json.put("downloadPath", getDownloadPath());
      json.put("pvalueCutoff", getPvalueCutoff());
      return json;
    }

  }

  public static class ResultRow {

    private String _word;
    private String _descrip;
    private String _bgdGenes;
    private String _resultGenes;
    private String _percentInResult;
    private String _foldEnrich;
    private String _oddsRatio;
    private String _pValue;
    private String _benjamini;
    private String _bonferroni;

    public ResultRow(String word, String descrip, String bgdGenes, String resultGenes, String percentInResult, String foldEnrich, String oddsRatio, String pValue, String benjamini, String bonferroni) {
      _word = word;
      _descrip = descrip;
      _bgdGenes = bgdGenes;
      _resultGenes = resultGenes;
      _percentInResult = percentInResult;
      _foldEnrich = foldEnrich;
      _oddsRatio = oddsRatio;
      _pValue = pValue;
      _benjamini = benjamini;
      _bonferroni = bonferroni;
    }

    public String getWord() { return _word; }
    public String getDescrip() { return _descrip; }
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
      json.put("word", _word);
      json.put("pathwayName", _descrip);
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
