package org.apidb.apicommon.model.view;

import java.util.Arrays;
import java.util.Map;
import java.util.List;

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

import org.gusdb.fgputil.functional.FunctionalInterfaces.Predicate;
import org.gusdb.wdk.model.ontology.Ontology;
import org.gusdb.wdk.model.ontology.OntologyNode;

public class GeneViewHandler extends AltSpliceViewHandler {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(GeneViewHandler.class);

  private static final String GENE_FILTERED_STEP = "geneFilteredStep";
  private static final String USER_PREFERENCE_SUFFIX = "_geneview";

  // attribute categories we will or may remove (might be obsolete)
  //private static final String TRANSCRIPT_CATEGORY_NAME = "trans_parent";
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

  // remove attributes in summary that apply to Transcripts (in ontology: property "geneOrTranscript" = transcript)
  @Override
  protected void customizeAvailableAttributeTree(Step step, TreeNode<SelectableItem> root, WdkModel model) throws WdkModelException {
    Ontology ontology = model.getOntology("Categories");

    // (1) get summary attributes nodes in attribute tree (dont care about categories)
    List<TreeNode<SelectableItem>> sumAttr =  root.getLeafNodes();

    // (2) for each summary attribute 
    for(TreeNode<SelectableItem> a : sumAttr) {
      Predicate<OntologyNode> predicate = new IsTrAttrPredicate(a.getContents().getName());
      // (2.1) check if there is a node in ontology with a.name that is targetType(attribute) in scope(results) and geneOrTr(transcript)
      // (2.2) and remove if found
      if ( ontology.findFirst(predicate) != null)
        root.removeAll(new NameMatchPredicate(a.getContents().getName()));  // should be only 1
    }

    String[] questionTypes = step.getQuestion().getPropertyList(QUESTION_TYPE_PROPLIST_KEY);
    if (questionTypes != null && 
        (Arrays.asList(questionTypes).contains(TRANSCRIPT_QUESTION_PROP_NAME) || step.getQuestionName().contains(TRANSCRIPT_BOOLEAN_QUESTION_SUBSTR) )) {
      // assume that dynamic columns of transcript questions are transcript attributes and remove from gene view
      root.removeAll(new NameMatchPredicate(DYNAMIC_ATTRIB_CATEGORY_NAME));
    }
  }

  @Override
  protected AttributeField[] getLeftmostFields(StepBean stepBean) throws WdkModelException {
    return new AttributeField[]{ stepBean.getQuestion().getRecordClass()
        .getPrimaryKeyAttribute().getPrimaryKeyAttributeField() };
  }

  @Override
  protected void customizeModelForView(Map<String, Object> model, StepBean stepBean) {
    // pass the new step to the JSP to be rendered instead of the normal step
    model.put(GENE_FILTERED_STEP, stepBean);
  }

  // =======================================================================
  private class IsTrAttrPredicate implements Predicate<OntologyNode> {
    String name;
    IsTrAttrPredicate(String name) {
      this.name = name;
    }   
    @Override
      public boolean test(OntologyNode node) {
        return node.containsKey("scope") && node.get("scope").contains("results") && 
             node.containsKey("targetType") && node.get("targetType").contains("attribute") && 
             node.containsKey("recordClassName") && node.get("recordClassName").contains("TranscriptRecordClasses.TranscriptRecordClass") && 
             node.containsKey("name") && node.get("name").contains(name) &&
             node.containsKey("geneOrTranscript") && node.get("geneOrTranscript").contains("transcript");
      }
  }


}
