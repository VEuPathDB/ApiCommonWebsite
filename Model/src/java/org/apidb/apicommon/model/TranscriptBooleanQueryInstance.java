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
		  String geneSql = super.getUncachedSql();
		  String sql = 
				  "WITH (" + geneSql + ") as genes" + NL +
				  "select gene_source_id, transcript_source, weight, sum(left_match) as left_match, sum(right_match) as right_match" + NL +
				  "from (" + NL +
				  "select left.gene_source_id, left.transcript_source_id, genes.weight, 1 as left_match, 0 as right_match" + NL +
				  "from genes, " + NL +
				  "(leftOperand) left" + NL +
				  "where left.gene_source_id = genes.gene_source_id" + NL +
				  "union" + NL +
				  "select left.gene_source_id, left.transcript_source_id, genes.weight, 1 as left_match, 0 as right_match" + NL +
				  "from genes, " + NL +
				  "(leftOperand) left" + NL +
				  "where left.gene_source_id = genes.gene_source_id" + NL +
				  "union" + NL +
				  "select ta.gene_source_id, ta.transcript_source_id, genes.weight, 0 as left_match, 0 as right_match" + NL +
				  "from genes, apidbtuning.transcriptattributes ta" + NL +
				  "where genes.gene_source_id = ta.gene_source_id) big" + NL +
				  "group by (gene_source_id, transcript_source, weight)";
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
