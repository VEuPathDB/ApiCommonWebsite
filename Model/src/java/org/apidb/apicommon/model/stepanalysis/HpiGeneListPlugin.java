package org.apidb.apicommon.model.stepanalysis;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import org.gusdb.fgputil.FormatUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
// import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;

import org.apidb.apicommon.model.stepanalysis.EnrichmentPluginUtil.Option; // The static class here should be factored

public class HpiGeneListPlugin extends AbstractSimpleProcessAnalyzer {

  private static final Logger LOG = Logger.getLogger(HpiGeneListPlugin.class);

    // Servers for the pick list
    // NOTE: you also need to add new bits to two additional places below
  private static final String EUPATH_NAME_KEY = "EuPathDB";
  private static final String EUPATH_SEARCH_SERVER_ENDPOINT_PROP_KEY = "eupathSearchServerEndpoint";
  private static final String PATRIC_NAME_KEY = "PATRIC";
  private static final String PATRIC_SEARCH_SERVER_ENDPOINT_PROP_KEY = "patricSearchServerEndpoint";
  private static final String EUPATH_PORTAL_NAME_KEY = "EuPathDB Portal";
  private static final String EUPATH_PORTAL_SEARCH_SERVER_ENDPOINT_PROP_KEY = "eupathSearchPortalEndpoint";

  private static final String BRC_PARAM_KEY = "brcParam";
  private static final String THRESHOLD_TYPE_PARAM_KEY = "thresholdTypeParam";
  private static final String THRESHOLD_PARAM_KEY = "thresholdParam";
  private static final String USE_ORTHOLOGY_PARAM_KEY = "useOrthologyParam";

  private static final String TABBED_RESULT_FILE_PATH = "hpiGeneListResult.tab";

  private static final String PROJECT_ID_KEY = "@PROJECT_ID@";


    private Map<String, String> serverEndpoints = new HashMap<String, String>();

    @Override
    public void validateProperties() throws WdkModelException {
        this.serverEndpoints.put(EUPATH_NAME_KEY, getProperty(EUPATH_SEARCH_SERVER_ENDPOINT_PROP_KEY));        
        this.serverEndpoints.put(PATRIC_NAME_KEY, getProperty(PATRIC_SEARCH_SERVER_ENDPOINT_PROP_KEY));        
        this.serverEndpoints.put(EUPATH_PORTAL_NAME_KEY, getProperty(EUPATH_PORTAL_SEARCH_SERVER_ENDPOINT_PROP_KEY));        
        // TODO ... Add more for other BRCs
    }       

    private Map<String, String> getServerEndpoints() {
        return this.serverEndpoints;
    }

  @Override
  public ValidationErrors validateFormParams(Map<String, String[]> formParams) throws WdkModelException, WdkUserException {

    ValidationErrors errors = new ValidationErrors();

    if (!formParams.containsKey(THRESHOLD_PARAM_KEY)) {
      errors.addParamMessage(THRESHOLD_PARAM_KEY, "Missing required parameter.");
    }
    else {
      try {
        double thresholdCutoff = Double.parseDouble(formParams.get(THRESHOLD_PARAM_KEY)[0]);
        if (thresholdCutoff <= 0 ) throw new NumberFormatException();
      }
      catch (NumberFormatException e) {
        errors.addParamMessage(THRESHOLD_PARAM_KEY, "Must be a number greater than 0.");
      }
    }
    return errors;
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException, WdkUserException {
      
      WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
      Map<String,String[]> params = getFormParams();

      String type = "gene"; 
      String idSource = "ensemble";
      
      String idSql =  "select distinct gene_source_id from (" + answerValue.getIdSql() + ")";

      String threshold = params.get(THRESHOLD_PARAM_KEY)[0];

      String brcValue = params.get(BRC_PARAM_KEY)[0];
      String searchServerEndpoint = this.serverEndpoints.get(brcValue);

      String thresholdType = params.get(THRESHOLD_TYPE_PARAM_KEY)[0];
      String useOrthology = params.get(USE_ORTHOLOGY_PARAM_KEY)[0];

      // create another path here for the image word cloud JP LOOK HERE name it like imageFilePath
      Path resultFilePath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);

      String qualifiedExe = Paths.get(GusHome.getGusHome(), "bin", "hpiGeneList.pl").toString();
      LOG.info(qualifiedExe + " "
               + idSql + " "
               +  thresholdType + " "
               +  threshold + " "
               +  useOrthology + " "
               +  type + " "
               +  idSource + " "
               + resultFilePath.toString() + " "
               + wdkModel.getProjectId() + " "
               + searchServerEndpoint
               );

      //TODO:  Add server endpoint
      return new String[]{ qualifiedExe, idSql, thresholdType, threshold, useOrthology, type, idSource, resultFilePath.toString(), wdkModel.getProjectId(), searchServerEndpoint};
  }


  @Override
  public Object getFormViewModel() throws WdkModelException, WdkUserException {

    List<Option> brcOptions = new ArrayList<>();
    brcOptions.add(new Option(EUPATH_NAME_KEY, EUPATH_NAME_KEY));
    brcOptions.add(new Option(EUPATH_PORTAL_NAME_KEY, EUPATH_PORTAL_NAME_KEY));
    brcOptions.add(new Option(PATRIC_NAME_KEY, PATRIC_NAME_KEY));

    List<Option> thresholdTypeOptions = new ArrayList<>();
    thresholdTypeOptions.add(new Option("percent_matched", "Percent Matched"));

    List<Option> useOrthologyOptions = new ArrayList<>();
    useOrthologyOptions.add(new Option("false", "No"));
    useOrthologyOptions.add(new Option("true", "Yes"));

    return new FormViewModel(brcOptions, thresholdTypeOptions, useOrthologyOptions, getWdkModel().getProjectId());
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    Path inputPath = Paths.get(getStorageDirectory().toString(), TABBED_RESULT_FILE_PATH);

    String brcValue = getFormParams().get(BRC_PARAM_KEY)[0];

    List<ResultRow> results = new ArrayList<>();
    try (FileReader fileIn = new FileReader(inputPath.toFile());
         BufferedReader buffer = new BufferedReader(fileIn)) {
      while (buffer.ready()) {
        String line = buffer.readLine();
        String[] columns = line.split(TAB);
        results.add(new ResultRow(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5], columns[6], columns[7]));
      }
      return new ResultViewModel(TABBED_RESULT_FILE_PATH, results, getFormParams());
    }
    catch (IOException ioe) {
      throw new WdkModelException("Unable to process result file at: " + inputPath, ioe);
    }
  }

  public static class FormViewModel {

      private final String brcParamHelp = "Choose which website to search";
      private final String thresholdTypeParamHelp = "Metric used to determine if this gene list matches a study";
      private final String thresholdParamHelp = "This number is used as a cutoff when finding studies from a gene list";
      private final String useOrthologyParamHelp = "Should we extend the search to consider genes orthologous to ones in the input list?";

      private List<Option> brcOptions;
      private List<Option> thresholdTypeOptions;
      private List<Option> useOrthologyOptions;

      private String projectId;

      public FormViewModel(List<Option> brcOptions, List<Option> thresholdTypeOptions, List<Option> useOrthologyOptions, String projectId) {
          this.brcOptions = brcOptions;
          this.thresholdTypeOptions = thresholdTypeOptions;
          this.useOrthologyOptions = useOrthologyOptions;
          this.projectId = projectId;
      }

      public List<Option> getBrcOptions() {
          return this.brcOptions;
      }

      public List<Option> getThresholdTypeOptions() {
          return this.thresholdTypeOptions;
      }

      public List<Option> getUseOrthologyOptions() {
          return this.useOrthologyOptions;
      }

      public String getBrcParamHelp() { return this.brcParamHelp; }
      public String getThresholdTypeParamHelp() { return this.thresholdTypeParamHelp; }
      public String getThresholdParamHelp() { return this.thresholdParamHelp; }
      public String getUseOrthologyParamHelp() { return this.useOrthologyParamHelp; }
      
  }

  public static class ResultViewModel {

      private final ResultRow HEADER_ROW = new ResultRow("Experiment Identifier", "Species",  "Experiment Name", "Description", "Type", "URI", "Significance", "List_URI");

      private final ResultRow COLUMN_HELP = new ResultRow(
                                                                "Unique ID for this experiment",
                                                                "Species this experimental data applies to",
                                                                "Name for the experient",
                                                                "Details abou tthis experiment",
                                                                "What type of experiment was this",
                                                                "Where can I find more information about this experiment",
                                                                "Statistic used to identify this experiment",
                                                                "URI for the List"
                                                                );
      
    private List<ResultRow> resultData;
    private String downloadPath;
    private Map<String, String[]> formParams;

    public ResultViewModel(String downloadPath, List<ResultRow> resultData,
			   Map<String, String[]> formParams) {
      this.downloadPath = downloadPath;
      this.formParams = formParams;
      this.resultData = resultData;
    }

      public ResultRow getHeaderRow() {
          return this.HEADER_ROW;
      }

      public ResultRow getHeaderDescription() {
          return this.COLUMN_HELP;
      }

      public List<ResultRow> getResultData() {
          return this.resultData;
      }
      public String getDownloadPath() {
          return this.downloadPath;
      }
      public Map<String,String[]> getFormParams() {
          return this.formParams;
      }
  }

  public static class ResultRow {

      private String experimentId;
      private String species;
      private String experimentName;
      private String description;
      private String type;
      private String uri;
      private String significance;
      private String serverEndpoint;

      public ResultRow(String experimentId, String species, String experimentName, String description, String type, String uri, String significance, String serverEndpoint) {
        this.experimentId = experimentId;
        this.species = species;
        this.experimentName = experimentName;
        this.description = description;
        this.type = type;
        this.uri = uri;
        this.significance = significance;
        this.serverEndpoint = serverEndpoint;
    }

      public String getExperimentId() { return this.experimentId; }
      public String getSpecies() { return this.species; }
      public String getExperimentName() { return this.experimentName; }
      public String getDescription() { return this.description; }
      public String getType() { return this.type; }
      public String getUri() { return this.uri; }
      public String getSignificance() { return this.significance; }      
      public String getServerEndPoint() { return this.serverEndpoint; }      
  }
}
