package org.apidb.apicommon.model.view;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.controller.summary.ResultTablePaging;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.user.Step;

public class TranscriptViewHandler implements SummaryViewHandler {

  @Override
  public Map<String, Object> process(Step step, Map<String, String[]> parameters)
      throws WdkModelException, WdkUserException {

    UserBean user = new UserBean(step.getUser());
    StepBean stepBean = new StepBean(user, step);
    AnswerValueBean answer = stepBean.getViewAnswerValue();
    answer.getRecords();
    Map<String, Object> model = ResultTablePaging.processPaging(
        parameters, stepBean.getQuestion(), user, answer);

    // figure out if transcript-only filter is on and inform view to check checkbox accordingly
    boolean filterOn = (step.getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);

    model.put(RepresentativeTranscriptFilter.FILTER_NAME, filterOn);

    return model;
  }

}
