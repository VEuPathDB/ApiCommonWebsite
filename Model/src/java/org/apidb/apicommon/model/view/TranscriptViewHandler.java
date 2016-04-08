package org.apidb.apicommon.model.view;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.json.JSONObject;

public class TranscriptViewHandler extends AbstractTranscriptViewHandler {

  private static final boolean REPRESENTATIVE_TRANSCRIPT_FILTER_ON_BY_DEFAULT = false;

  private static boolean shouldEngageFilter(User user) {
    // get user preference
    String prefValue = user.getProjectPreferences().get(RepresentativeTranscriptFilter.FILTER_NAME);
    return (prefValue == null ? REPRESENTATIVE_TRANSCRIPT_FILTER_ON_BY_DEFAULT : Boolean.valueOf(prefValue));
  }
  
  @Override
  protected String getUserPreferenceSuffix() {
    // this view uses the default preference suffix (i.e. the empty string)
    return User.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX;
  }

  @Override
  protected Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException {

    // read from step if transcript-only filter is turned on...
    boolean filterOnInStep = (step.getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);

    boolean shouldEngageFilter = shouldEngageFilter(user);

    // use passed step value if matches preference; otherwise toggle
    if (filterOnInStep == shouldEngageFilter) {
      return step;
    }

    Step stepCopy = new Step(step);
    if (shouldEngageFilter) {
      // add view filter
      stepCopy.addViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME, new JSONObject());
    }
    else {
      // remove view filter (already present)
      stepCopy.removeViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME);
    }

    return stepCopy;
  }

  /**
   * Adds model value for transcript filter checkbox
   */
  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) throws WdkModelException {
    // model contains the "model" for this view (does not relate to wdkModel)
    model.put(RepresentativeTranscriptFilter.FILTER_NAME, shouldEngageFilter(stepBean.getUser().getUser()));
  }

}
