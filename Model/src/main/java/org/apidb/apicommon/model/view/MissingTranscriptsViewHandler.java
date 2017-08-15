package org.apidb.apicommon.model.view;

import java.util.HashMap;
import java.util.Map;

import org.gusdb.wdk.controller.summary.SummaryTableUpdateProcessor;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class MissingTranscriptsViewHandler extends AbstractTranscriptViewHandler {

  private static final String USER_PREFERENCE_SUFFIX = "_missingTranscriptsView";
  private static final String MISSING_TRANSCRIPTS_STEP = "missingTranscriptsStep";

  @Override
  protected String getUserPreferenceSuffix() {
    return USER_PREFERENCE_SUFFIX;
  }

  /**
   * Note: Currently this view uses a transform to build the result.  Using this
   *   method means question-specific columns that the user might expect to see
   *   see (e.g. matches this step, matches previous step) will not be present.
   *   To overcome this, we would need to convert this implementation to use a
   *   view filter to achieve the same results as the transform.
   */
  @Override
  protected Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException {
    // get new step, to make result view from
    // use original step id, so that state in this view is associated with it
    Step newStep = new Step(wdkModel.getStepFactory(), user, step.getStepId());
    newStep.setInMemoryOnly(true);
    newStep.setQuestionName("InternalQuestions.GenesByMissingTranscriptsTransform");
    Map<String, String> paramValues = new HashMap<String, String>();
    paramValues.put("gene_result", String.valueOf(step.getStepId()));
    newStep.setParamValues(paramValues);
    return newStep;
  }

  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) throws WdkModelException {
    // pass the new step to the JSP to be rendered instead of the normal step
    model.put(MISSING_TRANSCRIPTS_STEP, stepBean);
  }

  /**
   * We are overriding processUpdate() for this view because the sorting and
   * attribute preferences fetched during view for the modified question above
   * (with the transform question name), thus, must use an identical step when
   * saving user preferences too.
   */
  @Override
  public String processUpdate(Step step, Map<String, String[]> parameters, User user, WdkModel wdkModel)
      throws WdkModelException, WdkUserException {
    Step customizedStep = customizeStep(step, user, wdkModel);
    return SummaryTableUpdateProcessor.processUpdates(customizedStep,
        parameters, user, wdkModel, getUserPreferenceSuffix());
  }
}
