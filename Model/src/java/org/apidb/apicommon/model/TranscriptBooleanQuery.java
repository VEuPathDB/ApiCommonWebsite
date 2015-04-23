package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.query.Column;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.attribute.PrimaryKeyAttributeField;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.question.DynamicAttributeSet;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.attribute.ColumnAttributeField;
import org.apache.log4j.Logger;


public class TranscriptBooleanQuery extends BooleanQuery {

  private static final Logger logger = Logger.getLogger(TranscriptBooleanQuery.class);

	public static final String LEFT_MATCH_COLUMN = "left_match";
	public static final String RIGHT_MATCH_COLUMN = "right_match";

    public TranscriptBooleanQuery() throws WdkModelException {
	super();
    }

    protected TranscriptBooleanQuery(TranscriptBooleanQuery tbq) {
	super(tbq);
    }

    private void addDynamicAttributeSetToQuestion(WdkModel wdkModel) throws WdkModelException {
       	DynamicAttributeSet das = getContextQuestion().getDynamicAttributeSet();
	
    	ColumnAttributeField left_af = new ColumnAttributeField();
	left_af.excludeResources(wdkModel.getProjectId());
    	left_af.setName(LEFT_MATCH_COLUMN);
    	left_af.setDisplayName("Matches prev step");
    	das.addAttributeField(left_af);
    	
    	ColumnAttributeField right_af = new ColumnAttributeField();
	right_af.excludeResources(wdkModel.getProjectId());
    	right_af.setName(RIGHT_MATCH_COLUMN);
    	right_af.setDisplayName("Matches this step");
    	das.addAttributeField(right_af);
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

    @Override
	public void setContextQuestion(Question contextQuestion) throws WdkModelException {
	super.setContextQuestion(contextQuestion);
	addDynamicAttributeSetToQuestion(wdkModel);
    }
    
    protected void prepareColumns(RecordClass recordClass) {
	super.prepareColumns(recordClass);
	
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
