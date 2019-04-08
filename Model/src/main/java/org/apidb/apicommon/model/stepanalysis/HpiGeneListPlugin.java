package org.apidb.apicommon.model.stepanalysis;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gusdb.fgputil.FormatUtil.TAB;

public class HpiGeneListPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(HpiGeneListPlugin.class);

  // Servers for the pick list
  // NOTE: you also need to add new bits to two additional places below
  private static final String EUPATH_NAME_KEY = "eupathSearchServerName";
  private static final String EUPATH_SEARCH_SERVER_ENDPOINT_PROP_KEY = "eupathSearchServerEndpoint";
  private static final String PATRIC_NAME_KEY = "patricSearchServerName";
  private static final String PATRIC_SEARCH_SERVER_ENDPOINT_PROP_KEY = "patricSearchServerEndpoint";
  private static final String VBASE_NAME_KEY = "vbaseSearchServerName";
  private static final String VBASE_SEARCH_SERVER_ENDPOINT_PROP_KEY = "vbaseSearchServerEndpoint";
  private static final String EUPATH_PORTAL_NAME_KEY = "eupathSearchPortalName";
  private static final String EUPATH_PORTAL_SEARCH_SERVER_ENDPOINT_PROP_KEY = "eupathSearchPortalEndpoint";

  private static final String BRC_PARAM_KEY = "brcParam";
  private static final String THRESHOLD_TYPE_PARAM_KEY = "thresholdTypeParam";
  private static final String THRESHOLD_PARAM_KEY = "thresholdParam";

  private static final String DS_CUTOFF_TYPE_PARAM_KEY = "datasetCutoffTypeParam";
  private static final String DS_CUTOFF_PARAM_KEY = "datasetCutoffParam";

  private static final String USE_ORTHOLOGY_PARAM_KEY = "useOrthologyParam";

  private static final String TABBED_RESULT_FILE_PATH = "hpiGeneListResult.tab";

  private Map<String, String> serverEndpoints = new HashMap<>();

  @Override
  public void validateProperties() {
    this.serverEndpoints.put(EUPATH_NAME_KEY, getProperty(EUPATH_SEARCH_SERVER_ENDPOINT_PROP_KEY));
    this.serverEndpoints.put(PATRIC_NAME_KEY, getProperty(PATRIC_SEARCH_SERVER_ENDPOINT_PROP_KEY));
    this.serverEndpoints.put(VBASE_NAME_KEY, getProperty(VBASE_SEARCH_SERVER_ENDPOINT_PROP_KEY));
    this.serverEndpoints.put(EUPATH_PORTAL_NAME_KEY, getProperty(EUPATH_PORTAL_SEARCH_SERVER_ENDPOINT_PROP_KEY));
    // TODO ... Add more for other BRCs
  }

  @Override
  public ValidationErrors validateFormParamValues(Map<String, String[]> formParams) {

    ValidationBundleBuilder errors = ValidationBundle.builder(ValidationLevel.SEMANTIC);

    if (!formParams.containsKey(THRESHOLD_PARAM_KEY)) {
      errors.addError(THRESHOLD_PARAM_KEY, "Missing required parameter.");
    } else {
      try {
        double thresholdCutoff = Double.parseDouble(formParams.get(THRESHOLD_PARAM_KEY)[0]);
        if (thresholdCutoff <= 0) throw new NumberFormatException();
      } catch (NumberFormatException e) {
        errors.addError(THRESHOLD_PARAM_KEY, "Must be a number greater than 0.");
      }
    }

    if (!formParams.containsKey(DS_CUTOFF_PARAM_KEY)) {
      errors.addError(DS_CUTOFF_PARAM_KEY, "Missing required parameter.");
    } else {
      try {
        double datasetCutoff = Double.parseDouble(formParams.get(DS_CUTOFF_PARAM_KEY)[0]);
        if (datasetCutoff <= 0) throw new NumberFormatException();
      } catch (NumberFormatException e) {
        errors.addError(DS_CUTOFF_PARAM_KEY, "Must be a number greater than 0.");
      }
    }

    return errors.build();
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    Map<String, String[]> params = getFormParams();

    String type = "gene";

    String idSource = "alt_locus_tag";

    String idSql = "select distinct gene_source_id from (" + answerValue.getIdSql() + ")";

    String threshold = params.get(THRESHOLD_PARAM_KEY)[0];

    String brcValue = params.get(BRC_PARAM_KEY)[0];
    String searchServerEndpoint = this.serverEndpoints.get(brcValue);

    String thresholdType = params.get(THRESHOLD_TYPE_PARAM_KEY)[0];
    String useOrthology = params.get(USE_ORTHOLOGY_PARAM_KEY)[0];

    String datasetCutoff = params.get(DS_CUTOFF_PARAM_KEY)[0];
    String datasetCutoffType = params.get(DS_CUTOFF_TYPE_PARAM_KEY)[0];


    // create another path here for the image word cloud JP LOOK HERE name it like imageFilePath
    Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);

    String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "hpiGeneList.pl").toString();
    LOG.info(qualifiedExe + " "
            + idSql + " "
            + thresholdType + " "
            + threshold + " "
            + datasetCutoffType + " "
            + datasetCutoff + " "
            + useOrthology + " "
            + type + " "
            + idSource + " "
            + resultFilePath.toString() + " "
            + wdkModel.getProjectId() + " "
            + searchServerEndpoint + " "
        //	       + orderbyp
    );

    //TODO:  Add server endpoint
    return new String[]{qualifiedExe, idSql, thresholdType, threshold, datasetCutoffType, datasetCutoff, useOrthology, type, idSource, resultFilePath.toString(), wdkModel.getProjectId(), searchServerEndpoint};
  }

  @Override
  public JSONObject getFormViewModelJson() throws WdkModelException {
    // This is now declared as parameters in the model xml
    return null;
  }

  @Override
  public JSONObject getResultViewModelJson() throws WdkModelException {
    return createResultViewModel().toJson();
  }


  private ResultViewModel createResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);

    //String brcValue = getFormParams().get(BRC_PARAM_KEY)[0];
    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      while (buffer.ready()) {
        String line = buffer.readLine();
        //LOG.info("LINE = " + line);
        String[] columns = line.split(TAB);

        results.add(new ResultRow(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5], columns[6], columns[7], columns[8], columns[9], columns[10], columns[11], columns[12]));
      }
	      /*
	      results.forEach(ResultRow -> {
		      LOG.info("The P_value Before Sort is:BBBBBefore" + ResultRow.getSignificance());
		  });
	      Collections.sort(results); // sort out p-value
	      results.forEach(ResultRow -> {
		      LOG.info("The P_value After Sort is: AAAAAfter" + ResultRow.getSignificance());
		  });
	      */

      Collections.sort(results); // sort out by p-value

      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams());

    } catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class ResultViewModel {

    private final ResultRow HEADER_ROW = new ResultRow(
        "Experiment Identifier",
        "Species",
        "Experiment Name",
        "Description",
        "Type",
        "URI",
        "Observed overlap",
        "Expected overlap",
        "Fold enrichment",
        "Percent of overlapping genes in your result",
        "Percent of this experiment genes in bkgd",
        "P-value",
        "List_URI"
    );

    private final ResultRow COLUMN_HELP = new ResultRow(
        "Unique ID for this experiment",
        "Species this experimental data applies to",
        "Name for the experient",
        "Details about this experiment",
        "What type of experiment was this",
        "Where can I find more information about this experiment",
        "The observed number of overlapping genes in your gene set and in this particular experiment",
        "The expected number of overlapping genes in your gene set and this particular experiment",
        "The observed overlap divided by the expected overlap",
        "The number of overlapping genes divided by the number of genes in your gene set",
        "The number of genes in this experiment meet the fold change criteria divided by the number of genes in background genome",
        "Statistic used to identify this experiment",
        "URI for the List"
    );

    private List<ResultRow> resultData;
    private String downloadPath;
    private Map<String, String[]> formParams;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData, Map<String, String[]> formParams) 
      {
	  this.downloadPath = downloadPath;
	  this.formParams = formParams;
	  //this.resultData = resultData;
	  this.resultData = Collections.unmodifiableList(resultData);
      }
      
      public ResultRow getHeaderRow() {
          return this.HEADER_ROW;
      }

      public ResultRow getHeaderDescription() {
          return this.COLUMN_HELP;
      }

      public List<ResultRow> getResultData() {
	  /*	  resultData.forEach(ResultRow -> {
	  	  LOG.info("The Final P_value is:" + ResultRow.getSignificance());
		  });
	  */
          return this.resultData;
      }
      public String getDownloadPath() {
          return this.downloadPath;
      }
      public Map<String,String[]> getFormParams() {
          return this.formParams;
      }
      
      public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("headerRow", getHeaderRow());
        json.put("headerDescription", getHeaderDescription());
        JSONArray resultsJson = new JSONArray();
        for (ResultRow rr : getResultData()) resultsJson.put(rr.toJson());
        json.put("resultData", resultsJson);
        json.put("downloadPath", getDownloadPath());
        return json;
      }
  }

  public static class ResultRow implements Comparable<Object> {

    private String experimentId;
    private String species;
    private String experimentName;
    private String description;
    private String type;
    private String uri;
    private String c11;
    private String c22;
    private String c33;
    private String c44;
    private String c55;
    private String significance;
    private String serverEndpoint;

    public ResultRow(String experimentId, String species, String experimentName, String description, String type, String uri, String c11, String c22, String c33, String c44, String c55, String significance, String serverEndpoint) {

      this.experimentId = experimentId;
      this.species = species;
      this.experimentName = experimentName;
      this.description = description;
      this.type = type;
      this.uri = uri;
      this.c11 = c11;
      this.c22 = c22;
      this.c33 = c33;
      this.c44 = c44;
      this.c55 = c55;
      this.significance = significance;
      this.serverEndpoint = serverEndpoint;
    }

    //	@Override
    @Override
    public int compareTo(Object o) {
      ResultRow r = (ResultRow) o;
      if (Double.parseDouble(this.significance) == Double.parseDouble(r.significance)) {
        return 0;
      } else if (Double.parseDouble(this.significance) < Double.parseDouble(r.significance)) {
        return -1;
      } else {
        return 1;
      }
    }

    public String getExperimentId() {
      return this.experimentId;
    }

    public String getSpecies() {
      return this.species;
    }

    public String getExperimentName() {
      return this.experimentName;
    }

    public String getDescription() {
      return this.description;
    }

    public String getType() {
      return this.type;
    }

    public String getUri() {
      return this.uri;
    }

    public String getC11() {
      return this.c11;
    }

    public String getC22() {
      return this.c22;
    }

    public String getC33() {
      return this.c33;
    }

    public String getC44() {
      return this.c44;
    }

    public String getC55() {
      return this.c55;
    }

    public String getSignificance() {
      return this.significance;
    }

    public String getServerEndPoint() {
      return this.serverEndpoint;
    }


    public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("experimentId", getExperimentId());
      json.put("species", getSpecies());
      json.put("experimentName", getExperimentName());
      json.put("description", getDescription());
      json.put("type", getType());
      json.put("uri", getUri());
      json.put("c11", getC11());
      json.put("c22", getC22());
      json.put("c33", getC33());
      json.put("c44", getC44());
      json.put("c55", getC55());
      json.put("significance", getSignificance());
      json.put("serverEndpoint", getServerEndPoint());

      return json;
    }
  }
}
