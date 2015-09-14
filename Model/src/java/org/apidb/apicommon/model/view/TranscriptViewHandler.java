package org.apidb.apicommon.model.view;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.controller.summary.ResultTablePaging;
import org.gusdb.wdk.controller.summary.SummaryTableUpdateProcessor;
import org.gusdb.wdk.model.TreeNode;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValueAttributes;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class TranscriptViewHandler implements SummaryViewHandler {

  private static final Logger LOG = Logger.getLogger(TranscriptViewHandler.class);

  private static final String TRANSCRIPT_ID_FIELD = "source_id";
  private static final String[] FIELDS_TO_REMOVE = { "gene_transcript_count", "transcripts_found_per_gene" };
  
  @Override
  public Map<String, Object> process(Step step, Map<String, String[]> parameters,
      User user, WdkModel wdkModel) throws WdkModelException, WdkUserException {

    // check to see if this request is asking to write summary attributes, sorting, or paging?
    LOG.info(FormatUtil.paramsToString(parameters));

    UserBean userBean = new UserBean(user);
    StepBean stepBean = new StepBean(userBean, step);
    AnswerValueBean answer = stepBean.getViewAnswerValue();
    answer.getRecords();
    Map<String, Object> model = ResultTablePaging.processPaging(
        parameters, stepBean.getQuestion(), userBean, answer);

    // override default and available columns
    AnswerValueAttributes attributes = answer.getAnswerValue().getAttributes();
    TreeNode root = attributes.getDisplayableAttributeTree();
    for (String fieldName : FIELDS_TO_REMOVE) {
      root.remove(fieldName);
    }
    attributes.overrideDisplayableAttributeTree(root);

    // override summary attributes
    AttributeField pkField = step.getQuestion().getRecordClass().getAttributeFieldMap().get(TRANSCRIPT_ID_FIELD);
    pkField = pkField.clone();
    pkField.setRemovable(false);
    Map<String, AttributeField> transcriptViewAttribs = AnswerValueAttributes
        .buildSummaryAttributeFieldMap(user, step.getQuestion(), User.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX, pkField);
    for (String fieldName : FIELDS_TO_REMOVE) {
      transcriptViewAttribs.remove(fieldName);
    }
    attributes.overrideSummaryAttributeFieldMap(transcriptViewAttribs);

    // figure out if transcript-only filter is on and inform view to check checkbox accordingly
    boolean filterOn = (step.getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);

    model.put(RepresentativeTranscriptFilter.FILTER_NAME, filterOn);

    return model;
  }

  @Override
  public String processUpdate(Step step, Map<String, String[]> parameters, User user, WdkModel wdkModel)
      throws WdkModelException, WdkUserException {
    return SummaryTableUpdateProcessor.processUpdates(step, parameters, user,
        wdkModel, User.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX);
  }

}
