package org.apidb.apicommon.model.stepanalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class FormTestPlugin extends AbstractStepAnalyzer {

  private static final Logger LOG = Logger.getLogger(FormTestPlugin.class);
  
  @Override
  public List<String> validateFormParams(Map<String, String[]> formParams) {
    return null;
  }
  
  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException {
    Map<String,String[]> params = getFormParams();
    Map<String,String> prettyParams = new HashMap<>();
    for (String key : params.keySet()) {
      prettyParams.put(key, FormatUtil.arrayToString(params.get(key)));
    }
    String result = FormatUtil.prettyPrint(prettyParams, Style.MULTI_LINE);
    LOG.info("Form test plugin setting following result:\n" + result);
    setPersistentCharData(result);
    return ExecutionStatus.COMPLETE;
  }

  @Override
  public Object getResultViewModel() {
    return getPersistentCharData();
  }

}
