package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.user.User;

public class TranscriptBooleanQueryInstance extends BooleanQueryInstance {
	private static final String NL = System.lineSeparator();
	  
	public TranscriptBooleanQueryInstance(User user, BooleanQuery query,
			Map<String, String> values, boolean validate, int assignedWeight,
			Map<String, String> context) throws WdkModelException,
			WdkUserException {
		super(user, query, values, validate, assignedWeight, context);
	}
	
	/*
	   * (non-Javadoc)
	   * 
	   * @see org.gusdb.wdk.model.query.SqlQueryInstance#getUncachedSql()
	   */
	  @Override
	  public String getUncachedSql() throws WdkModelException, WdkUserException {
		  String sql = super.getUncachedSql();
		return sql;	
		  
	  }

	  /**
	   * get the columns to do the boolean operation on
	   * subclasses can override to provide custom pk columns
	   * @return
	   */
	  @Override
	  protected String[] getPkColumns() {
		  String[] cols = {"gene_source_id"};
		  return cols;
	  }
	  
	  /**
	   * subclasses can override to provide custom preprocessing
	   * @param operandSql
	   * @return
	   */
	  @Override
	  protected String preProcessOperandSql(String operandSql, String leftOrRight) {
		  String sql = "-- reduce to genes for boolean" + NL + 
				  "select distinct gene_source_id, weight from (" + operandSql + ") genes" + leftOrRight;
		  return operandSql;
	  }
	  
}
