package org.apidb.apicommon.model.record.attribute.plugin;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.fgputil.ArrayUtil;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.record.attribute.plugin.WordCloudAttributePlugin;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class ApiWordCloudAttributePlugin extends WordCloudAttributePlugin {

  private static String[] SUPPLEMENTAL_COMMON_WORDS = {
      "off", "cgi", "bin", "groupac", "href", "http", "org", "tmp",
      "chro", "sequencelist", "orthomcl", "orthomclweb"
  };

  @Override
  protected AnswerValue getAnswerValue(Step step, User user) throws WdkModelException, WdkUserException {
    if (TranscriptUtil.isTranscriptQuestion(step.getQuestion())) {
      // transcript question; see if we should apply one-transcript-per-gene filter
      step = RepresentativeTranscriptFilter.applyToStepFromUserPreference(step, user);
    }
    return super.getAnswerValue(step, user);
  }

  @Override
  protected String[] getCommonWords() {
    return ArrayUtil.concatenate(COMMON_WORDS, SUPPLEMENTAL_COMMON_WORDS);
  }

}
