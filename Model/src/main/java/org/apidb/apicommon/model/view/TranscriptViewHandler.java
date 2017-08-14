package org.apidb.apicommon.model.view;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.user.UserPreferences;

public class TranscriptViewHandler extends AbstractTranscriptViewHandler {

  private static final String TRANSCRIPT_FILTERED_STEP = "modifiedStep";

  @Override
  protected String getUserPreferenceSuffix() {
    // this view uses the default preference suffix (i.e. the empty string)
    return UserPreferences.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX;
  }

  @Override
  protected Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException {
    return RepresentativeTranscriptFilter.applyToStepFromUserPreference(step, user);
  }

  /**
   * Adds model value for transcript filter checkbox
   */
  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) throws WdkModelException {
    // model contains the "model" for this view (does not relate to wdkModel)
    model.put(RepresentativeTranscriptFilter.FILTER_NAME,
        RepresentativeTranscriptFilter.shouldEngageFilter(stepBean.getUser().getUser()));
    // pass the new step to the JSP to be rendered instead of the normal step
    model.put(TRANSCRIPT_FILTERED_STEP, stepBean);
  }

}
