package org.apidb.apicommon.model.record.attribute.plugin;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.eupathdb.common.model.attribute.EuPathWordCloudAttributePlugin;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class ApiWordCloudAttributePlugin extends EuPathWordCloudAttributePlugin {

  @Override
  protected AnswerValue getAnswerValue(Step step, User user) throws WdkModelException, WdkUserException {
    if (TranscriptUtil.isTranscriptQuestion(step.getQuestion())) {
      // transcript question; see if we should apply one-transcript-per-gene filter
      step = RepresentativeTranscriptFilter.applyToStepFromUserPreference(step, user);
    }
    return super.getAnswerValue(step, user);
  }

}
