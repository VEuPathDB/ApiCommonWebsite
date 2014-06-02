package org.apidb.apicommon.model.stepanalysis;

import org.gusdb.wdk.model.TreeNode;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;

public class CheckboxTreeTestPlugin extends AbstractStepAnalyzer {

  private static final String CHECKBOX_TREE_KEY = "myTree";

  @Override
  public Object getFormViewModel() throws WdkModelException {
    TreeNode root = buildTreeParam();
    root.setDefaultLeaves("value2", "value3", "value6");
    return root;
  }

  private TreeNode buildTreeParam() {
    TreeNode root = new TreeNode("root", "Root", "This is the root value.");
    TreeNode branch1 = new TreeNode("branch1", "Branch 1");
    TreeNode val1 = new TreeNode("value1", "Value 1");
    TreeNode val2 = new TreeNode("value2", "Value 2");
    branch1.addChildNode(val1);
    branch1.addChildNode(val2);
    root.addChildNode(branch1);
    TreeNode branch2 = new TreeNode("branch2", "Branch 2");
    TreeNode val3 = new TreeNode("value3", "Value 3");
    TreeNode val4 = new TreeNode("value4", "Value 4");
    branch2.addChildNode(val3);
    branch2.addChildNode(val4);
    root.addChildNode(branch2);
    TreeNode val5 = new TreeNode("value5", "Value 5");
    TreeNode val6 = new TreeNode("value6", "Value 6");
    root.addChildNode(val5);
    root.addChildNode(val6);
    return root;
  }

  @Override
  public Object getResultViewModel() throws WdkModelException {
    StringBuilder result = new StringBuilder("Items selected:\n\n");
    for (String option : getFormParams().get(CHECKBOX_TREE_KEY)) {
      result.append(option).append("\n");
    }
    return result.toString();
  }

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException {
    // nothing to do; just testing retention of checkbox tree values
    return ExecutionStatus.COMPLETE;
  }
}
