package org.apidb.apicommon.model.stepanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.analysis.ValidationErrors;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.json.JSONObject;

public class ListProcessPlugin extends AbstractSimpleProcessAnalyzer {
  
  private static final Logger LOG = Logger.getLogger(ListProcessPlugin.class);
  
  private static final String LIST_EXECUTABLE = "/bin/ls";
  private static final String LIST_OPTIONS = "-alF";
  private static final String LOCATION_PARAM = "location";
  
  @Override
  public ValidationErrors validateFormParams(Map<String,String[]> params) {
    ValidationErrors errors = new ValidationErrors();
    String[] vals = params.get(LOCATION_PARAM);
    if ( vals == null || vals.length != 1 || vals[0].isEmpty()) {
      errors.addParamMessage(LOCATION_PARAM, "Location value cannot be empty.");
    }
    return errors;
  }
  
  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {
    return new String[]{ LIST_EXECUTABLE, LIST_OPTIONS, getFormParams().get(LOCATION_PARAM)[0] };
  }
  
  @Override
  public Object getFormViewModel() throws WdkModelException, WdkUserException {
    return null;
  }

  @Override
  public JSONObject getFormViewModelJson() throws WdkModelException {
    return new JSONObject();
  }

  
  @Override
  public Object getResultViewModel() {
    return createResultViewModel();
  }
  
  @Override
  public JSONObject getResultViewModelJson() {
    JSONObject json = new JSONObject();
    json.put("result", createResultViewModel());
    return json;
  }

  private String createResultViewModel() {
    String result = "";
    try(FileReader fr = new FileReader(getStdoutFilePath().toFile());
        BufferedReader br = new BufferedReader(fr)) {
      while(br.ready()) {
        result += br.readLine() + FormatUtil.NL;
      }
      return result;
    }
    catch (IOException e) {
      LOG.error("Unable to read from " + getStdoutFilePath(), e);
      return "Unable to read result.";
    }
  }
}
