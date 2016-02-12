package org.apidb.apicommon.model.view;

import java.util.Map;

import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class TranscriptViewHandler extends AbstractTranscriptViewHandler {

  @Override
  protected String getUserPreferenceSuffix() {
    // this view uses the default preference suffix (i.e. the empty string)
    return User.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX;
  }

  @Override
  protected Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException {
    // no step customization needed
    return step;
  }

  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) throws WdkModelException {

    // read from step if transcript-only filter is ...
    boolean filterOn = (stepBean.getStep().getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);

		// ... and inform view to check checkbox accordingly
		// model contains the "model" for this view (does not relate to wdkModel)
    model.put(RepresentativeTranscriptFilter.FILTER_NAME, filterOn);
  }

}
