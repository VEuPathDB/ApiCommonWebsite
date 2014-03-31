package org.apidb.apicommon.model.stepanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;

public class ListProcessPlugin extends AbstractSimpleProcessAnalyzer {
  
  private static final Logger LOG = Logger.getLogger(ListProcessPlugin.class);
  
  private static final String LIST_EXECUTABLE = "/bin/ls";
  private static final String LIST_OPTIONS = "-alF";
  private static final String LOCATION_PARAM = "location";
  
  @Override
  public List<String> validateFormParams(Map<String,String[]> params) {
    List<String> errors = new ArrayList<>();
    String[] vals = params.get(LOCATION_PARAM);
    if ( vals == null || vals.length != 1 || vals[0].isEmpty()) {
      errors.add("Location value cannot be empty.");
    }
    return errors;
  }
  
  @Override
  protected String[] getCommand(AnswerValue answerValue) {
    return new String[]{ LIST_EXECUTABLE, LIST_OPTIONS, getFormParams().get(LOCATION_PARAM)[0] };
  }
  
  @Override
  public Object getResultViewModel() {
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
