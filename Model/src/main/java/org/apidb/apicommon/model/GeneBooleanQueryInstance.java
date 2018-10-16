package org.apidb.apicommon.model;

import org.gusdb.fgputil.collection.ReadOnlyMap;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.user.User;

/**
 * Do boolean operations on gene_source_id.  This is a helper class for TranscriptBooleanQueryInstance
 * @author Steve
 *
 */
public class GeneBooleanQueryInstance extends BooleanQueryInstance {

  public GeneBooleanQueryInstance(User user, BooleanQuery query, ReadOnlyMap<String, String> values,
      int assignedWeight) throws WdkModelException {
    super(user, query, values, assignedWeight);
  }

  /**
   * get the columns to do the boolean operation on subclasses can override to provide custom pk columns
   * 
   * @return
   */
  @Override
  protected String[] getPkColumns() {
    String[] cols = { "gene_source_id", "project_id" };
    return cols;
  }
  
  /**
   * because we are using gene_source_id as primary key, and there are potentially multiple transcripts per gene,
   * we might have non-unique gene rows.  indicate this to the boolean query instance, so it can collapse them as needed
   */
  @Override
  protected boolean possiblyNonUniquePrimaryKeys() {
    return true;
  }

}
