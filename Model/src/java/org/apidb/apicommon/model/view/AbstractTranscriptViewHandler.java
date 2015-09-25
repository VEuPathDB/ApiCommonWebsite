package org.apidb.apicommon.model.view;

import java.util.Arrays;

import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.wdk.model.SelectableItem;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.FieldTree.NameMatchPredicate;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.record.attribute.AttributeField;

public abstract class AbstractTranscriptViewHandler extends AltSpliceViewHandler {

  private static final String TRANSCRIPT_ID_FIELD = "transcript_link";

  private static final String[] FIELDS_TO_REMOVE = {
    TRANSCRIPT_ID_FIELD, "gene_transcript_count", "transcripts_found_per_gene"
  };

  @Override
  protected void customizeAvailableAttributeTree(TreeNode<SelectableItem> root) {
    root.removeAll(new NameMatchPredicate(Arrays.asList(FIELDS_TO_REMOVE)));
  }

  @Override
  protected AttributeField getLeftmostField(StepBean stepBean) throws WdkModelException {
    AttributeField pkField = stepBean.getStep().getQuestion().getRecordClass()
        .getAttributeFieldMap().get(TRANSCRIPT_ID_FIELD);
    pkField = pkField.clone();
    pkField.setRemovable(false);
    return pkField;
  }
}
