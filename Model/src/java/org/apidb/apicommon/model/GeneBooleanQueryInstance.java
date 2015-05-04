package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.user.User;

/**
 * Do boolean operations on gene_source_id.  This is a helper class for TranscriptBooleanQueryInstance
 * @author Steve
 *
 */
public class GeneBooleanQueryInstance extends BooleanQueryInstance {
	  
	public GeneBooleanQueryInstance(User user, BooleanQuery query,
			Map<String, String> values, boolean validate, int assignedWeight,
			Map<String, String> context) throws WdkModelException,
			WdkUserException {
		super(user, query, values, validate, assignedWeight, context);
	}

	  /**
	   * get the columns to do the boolean operation on
	   * subclasses can override to provide custom pk columns
	   * @return
	   */
	  @Override
	  protected String[] getPkColumns() {
		  String[] cols = {"gene_source_id, project_id"};
		  return cols;
	  }
	  
}
