package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.query.Column;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.attribute.PrimaryKeyAttributeField;
import org.gusdb.wdk.model.user.User;

public class TranscriptBooleanQuery extends BooleanQuery {

	public static final String LEFT_MATCH_COLUMN = "left_match";
	public static final String RIGHT_MATCH_COLUMN = "right_match";

    public TranscriptBooleanQuery()
			throws WdkModelException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gusdb.wdk.model.query.Query#makeInstance()
	 */
	@Override
	public BooleanQueryInstance makeInstance(User user, Map<String, String> values,
			boolean validate, int assignedWeight, Map<String, String> context)
					throws WdkModelException, WdkUserException {
		return new TranscriptBooleanQueryInstance(user, this, values, validate,
				assignedWeight, context);
	}	

	protected void prepareColumns(RecordClass recordClass) {
		super.prepareColumns(recordClass);
		PrimaryKeyAttributeField primaryKey = recordClass.getPrimaryKeyAttributeField();
		
		Column column = new Column();
		column.setName(LEFT_MATCH_COLUMN);
		column.setQuery(this);
		columnMap.put(LEFT_MATCH_COLUMN, column);

		column = new Column();
		column.setName(RIGHT_MATCH_COLUMN);
		column.setQuery(this);
		columnMap.put(RIGHT_MATCH_COLUMN, column);

	}

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.Query#clone()
   */
  @Override
  public Query clone() {
    return new TranscriptBooleanQuery(this);
  }



}
