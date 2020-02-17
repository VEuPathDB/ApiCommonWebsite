package org.apidb.apicommon.model.stepanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.json.JSONObject;

public class ListProcessPlugin extends AbstractSimpleProcessAnalyzer {
  
  private static final Logger LOG = Logger.getLogger(ListProcessPlugin.class);
  
  private static final String LIST_EXECUTABLE = "/bin/ls";
  private static final String LIST_OPTIONS = "-alF";
  private static final String LOCATION_PARAM = "location";

  // TODO: verify that validation is being performed here (i.e. that these params live in the model
  @SuppressWarnings("unused")
  private ValidationBundle validateFormParamValues(Map<String,String[]> params) {
    ValidationBundleBuilder errors = ValidationBundle.builder(ValidationLevel.SEMANTIC);
    String[] vals = params.get(LOCATION_PARAM);
    if ( vals == null || vals.length != 1 || vals[0].isEmpty()) {
      errors.addError(LOCATION_PARAM, "Location value cannot be empty.");
    }
    return errors.build();
  }
  
  @Override
  protected String[] getCommand(AnswerValue answerValue) {
    return new String[]{ LIST_EXECUTABLE, LIST_OPTIONS, getFormParams().get(LOCATION_PARAM)[0] };
  }

  @Override
  public JSONObject getResultViewModelJson() {
    JSONObject json = new JSONObject();
    json.put("result", createResultViewModel());
    return json;
  }

  private String createResultViewModel() {
    StringBuilder result = new StringBuilder();
    try(BufferedReader br = new BufferedReader(new FileReader(getStdoutFilePath().toFile()))) {
      String line;
      while((line = br.readLine()) != null) {
        result.append(line).append(FormatUtil.NL);
      }
      return result.toString();
    }
    catch (IOException e) {
      LOG.error("Unable to read from " + getStdoutFilePath(), e);
      return "Unable to read result.";
    }
  }
}
