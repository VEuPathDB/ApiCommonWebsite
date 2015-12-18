package org.apidb.apicommon.model.view;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.filter.RepresentativeTranscriptFilter;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.wdk.model.FieldTree.NameMatchPredicate;
import org.gusdb.wdk.model.SelectableItem;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;
import org.json.JSONObject;

public class GeneViewHandler extends AltSpliceViewHandler {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(GeneViewHandler.class);

  private static final String GENE_FILTERED_STEP = "geneFilteredStep";
  private static final String USER_PREFERENCE_SUFFIX = "_geneview";

  // attribute categories we will or may remove
  private static final String TRANSCRIPT_CATEGORY_NAME = "trans_parent";
  private static final String DYNAMIC_ATTRIB_CATEGORY_NAME = "dynamic";

  // custom properties on gene questions
  private static final String QUESTION_TYPE_PROPLIST_KEY = "questionType";
  private static final String TRANSCRIPT_QUESTION_PROP_NAME = "transcript";
  private static final String TRANSCRIPT_BOOLEAN_QUESTION_SUBSTR = "boolean_question_Transcript";

  @Override
  protected String getUserPreferenceSuffix() {
    return USER_PREFERENCE_SUFFIX;
  }

  @Override
  protected Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException {
    boolean filterOn = (step.getViewFilterOptions()
        .getFilterOption(RepresentativeTranscriptFilter.FILTER_NAME) != null);
    // if filter is not already applied (i.e. by checkbox in transcript view), then add it to in-memory step
    if (!filterOn) {
      step = new Step(step);
      step.addViewFilterOption(RepresentativeTranscriptFilter.FILTER_NAME, new JSONObject());
    }
    return step;
  }

  @Override
  protected void customizeAvailableAttributeTree(Step step, TreeNode<SelectableItem> root) throws WdkModelException {
    root.removeAll(new NameMatchPredicate(TRANSCRIPT_CATEGORY_NAME));
    String[] questionTypes = step.getQuestion().getPropertyList(QUESTION_TYPE_PROPLIST_KEY);
    if (questionTypes != null && 
        (Arrays.asList(questionTypes).contains(TRANSCRIPT_QUESTION_PROP_NAME) || step.getQuestionName().contains(TRANSCRIPT_BOOLEAN_QUESTION_SUBSTR) )) {
      // assume that dynamic columns of transcript questions are transcript attributes and remove from gene view
      root.removeAll(new NameMatchPredicate(DYNAMIC_ATTRIB_CATEGORY_NAME));
    }
  }

  @Override
  protected AttributeField getLeftmostField(StepBean stepBean) throws WdkModelException {
    return stepBean.getQuestion().getRecordClass()
        .getPrimaryKeyAttribute().getPrimaryKeyAttributeField();
  }

  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) {
    // pass the new step to the JSP to be rendered instead of the normal step
    model.put(GENE_FILTERED_STEP, stepBean);
  }
}
