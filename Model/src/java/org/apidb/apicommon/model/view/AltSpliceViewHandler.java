package org.apidb.apicommon.model.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.wdk.controller.summary.ResultTablePaging;
import org.gusdb.wdk.controller.summary.SummaryTableUpdateProcessor;
import org.gusdb.wdk.model.FieldTree;
import org.gusdb.wdk.model.FieldTree.NameMatchPredicate;
import org.gusdb.wdk.model.SelectableItem;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.AnswerValueAttributes;
import org.gusdb.wdk.model.answer.SummaryViewHandler;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public abstract class AltSpliceViewHandler implements SummaryViewHandler {

  private static final Logger LOG = Logger.getLogger(AltSpliceViewHandler.class);

  protected static final String PRIMARY_KEY_FIELD = "primary_key";

  protected abstract String getUserPreferenceSuffix();
  protected abstract Step customizeStep(Step step, User user, WdkModel wdkModel) throws WdkModelException;
  protected abstract void customizeAvailableAttributeTree(Step step, TreeNode<SelectableItem> root) throws WdkModelException;
  protected abstract AttributeField[] getLeftmostFields(StepBean stepBean) throws WdkModelException;
  protected abstract void customizeModelForView(Map<String, Object> model, StepBean stepBean) throws WdkModelException;

  @Override
  public Map<String, Object> process(Step step, Map<String, String[]> parameters,
      User user, WdkModel wdkModel) throws WdkModelException, WdkUserException {

    // check to see if this request is asking to write summary attributes, sorting, or paging?
    LOG.info("Call to " + getClass().getSimpleName() + " with params: " + FormatUtil.paramsToString(parameters));

    // customize step params and any filters (legacy, normal, view)
    step = customizeStep(step, user, wdkModel);

    // create beans for convenience
    UserBean userBean = new UserBean(user);
    StepBean stepBean = new StepBean(userBean, step);

    // get initial view answer value; we will customize it next
    AnswerValue answer = step.getViewAnswerValue();

    // override sorting, but only if suffix is not default
    if (!getUserPreferenceSuffix().equals(User.DEFAULT_SUMMARY_VIEW_PREF_SUFFIX)) {
      answer.setSortingMap(user.getSortingAttributes(
          stepBean.getQuestion().getFullName(), getUserPreferenceSuffix()));
    }

    // set up paging based on request params
    Map<String, Object> model = ResultTablePaging.processPaging(
        parameters, stepBean.getQuestion(), userBean, new AnswerValueBean(answer));

    // get base available attributes and remove those not relevant to gene view
    AnswerValueAttributes attributes = answer.getAttributes();
    FieldTree tree = attributes.getDisplayableAttributeTree();
    TreeNode<SelectableItem> root = tree.getRoot();

    // customize attributes in the Add Columns pop-up
    customizeAvailableAttributeTree(step, root);

    // override summary attributes
    AttributeField[] leftmostFields = getLeftmostFields(stepBean);
    Map<String, AttributeField> summaryFields = AnswerValueAttributes.buildSummaryAttributeFieldMap(user, step.getQuestion(), getUserPreferenceSuffix(), leftmostFields);
    trimAttribsNotInTree(summaryFields, root, leftmostFields);
    attributes.overrideSummaryAttributeFieldMap(summaryFields);

    // assign currently selected columns to be selected nodes in Add Columns pop-up
    tree.setSelectedLeaves(new ArrayList<>(summaryFields.keySet()));
    attributes.overrideDisplayableAttributeTree(tree);

    // customize model (most likely assigning custom variable to a customized step
    customizeModelForView(model, stepBean);

    return model;
  }

  private static void trimAttribsNotInTree(Map<String, AttributeField> attributes,
      TreeNode<SelectableItem> attributeTree, AttributeField[] leftmostFields) {
    List<String> origNames = new ArrayList<>(attributes.keySet());
    for (String name : origNames) {
      // remove if not in tree, but don't remove primary key or chosen leftmost field
      if ( name.equals(PRIMARY_KEY_FIELD) ) continue;
      for(AttributeField a : leftmostFields) {
				if ( name.equals(a.getName()) ) continue;
			}
      if ( attributeTree.findFirst( new NameMatchPredicate(name) ) == null ) {
        attributes.remove(name);
      }
    }
  }

  @Override
  public String processUpdate(Step step, Map<String, String[]> parameters, User user, WdkModel wdkModel)
      throws WdkModelException, WdkUserException {
    return SummaryTableUpdateProcessor.processUpdates(step, parameters, user, wdkModel, getUserPreferenceSuffix());
  }
}
