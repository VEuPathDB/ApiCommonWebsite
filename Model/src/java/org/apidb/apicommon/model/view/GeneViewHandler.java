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
import org.json.JSONObject;

public class GeneViewHandler implements SummaryViewHandler {

  private static final String GENE_FILTERED_STEP = "geneFilteredStep";

  @Override
  public Map<String, Object> process(Step step, Map<String, String[]> parameters)
      throws WdkModelException, WdkUserException {

    boolean filterOn = (step.getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);

    // if filter is not already applied (i.e. by checkbox in transcript view), then add it to in-memory step
    if (!filterOn) {
      step = new Step(step);
      step.addViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME, new JSONObject());
    }

    UserBean user = new UserBean(step.getUser());
    StepBean stepBean = new StepBean(user, step);
    AnswerValueBean answer = stepBean.getViewAnswerValue();
    answer.getRecords();
    Map<String, Object> model = ResultTablePaging.processPaging(
        parameters, stepBean.getQuestion(), user, answer);

    model.put(GENE_FILTERED_STEP, stepBean);
    return model;
  }
}
