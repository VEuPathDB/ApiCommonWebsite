package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
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
		  String booleanTranscriptsSql = super.getUncachedSql();
                  String booleanGenesSql = "select ta.gene_source_id from (" + booleanTranscriptSql + ") bt, apidbtuning.transcriptattributes ta where bt.source_id = ta.source_id ";
		  String 
		  String sql = 
				  "WITH genes as (" + geneSql + ")" + NL +
				  "select gene_source_id, source_id, weight, sum(left_match) as " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", sum(right_match) as " + TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + NL +
				  "from (" + NL +
				  "  select left.gene_source_id, left.source_id, genes.weight, 1 as left_match, 0 as right_match" + NL +
				  "  from genes, " + NL +
				  "  (leftOperand) left" + NL +
				  "  where left.gene_source_id = genes.gene_source_id" + NL +
				  "  UNION" + NL +
				  "  select right.gene_source_id, right.source_id, genes.weight, 1 as right_match, 0 as right_match" + NL +
				  "  from genes, " + NL +
				  "  (rightOperand) right" + NL +
				  "  where right.gene_source_id = genes.gene_source_id" + NL +
				  "  UNION" + NL +
				  "  select ta.gene_source_id, ta.source_id, genes.weight, 0 as left_match, 0 as right_match" + NL +
				  "  from genes, apidbtuning.transcriptattributes ta" + NL +
				  "  where genes.gene_source_id = ta.gene_source_id) big" + NL +
				  "group by (gene_source_id, source_id, weight)";
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
