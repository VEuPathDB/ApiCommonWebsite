package org.apidb.apicommon.model.record.attribute.plugin;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.eupathdb.common.model.attribute.EuPathWordCloudAttributePlugin;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class ApiWordCloudAttributePlugin extends EuPathWordCloudAttributePlugin {

  @Override
  protected AnswerValue getAnswerValue(AnswerValue answerValue) throws WdkModelException, WdkUserException {
    if (TranscriptUtil.isTranscriptQuestion(answerValue.getAnswerSpec().getQuestion())) {
      // transcript question; see if we should apply one-transcript-per-gene filter
      return AnswerValueFactory.makeAnswer(answerValue,
          RepresentativeTranscriptFilter.applyToStepFromUserPreference(answerValue.getRunnableAnswerSpec(), answerValue.getUser()));
    }
    return answerValue;
  }

}
