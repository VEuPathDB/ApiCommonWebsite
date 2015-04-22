package org.apidb.apicommon.model;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.BooleanQueryInstance;
import org.gusdb.wdk.model.user.User;
import org.apache.log4j.Logger;


public class TranscriptBooleanQueryInstance extends BooleanQueryInstance {
    private static final String NL = System.lineSeparator();
  private static final Logger logger = Logger.getLogger(BooleanQueryInstance.class);

    GeneBooleanQueryInstance genebqi;
	  
    public TranscriptBooleanQueryInstance(User user, BooleanQuery query,
					  Map<String, String> values, boolean validate, int assignedWeight,
					  Map<String, String> context) throws WdkModelException,
									      WdkUserException {
	super(user, query, values, validate, assignedWeight, context);
	genebqi = new GeneBooleanQueryInstance(user, query, values, false, assignedWeight, context);
    }
	
    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.query.SqlQueryInstance#getUncachedSql()
     */
    @Override
	public String getUncachedSql() throws WdkModelException, WdkUserException {

	String booleanGenesSql = genebqi.getUncachedSql();

	String sql = 
	    " -- boolean of genes " + NL +
	    "WITH genes as (" + booleanGenesSql + ")" + NL +
	    " -- major select " + NL +
	    "select gene_source_id, source_id, project_id, wdk_weight, sum(left_match) as " + TranscriptBooleanQuery.LEFT_MATCH_COLUMN + ", sum(right_match) as " + TranscriptBooleanQuery.RIGHT_MATCH_COLUMN + NL +
	    "from (" + NL +
	    "  select left.gene_source_id, left.source_id, left.project_id, genes.wdk_weight, 1 as left_match, 0 as right_match" + NL +
	    "  from genes, " + NL +
	    "  (" + getLeftSql() + ") left" + NL +
	    "  where left.gene_source_id = genes.gene_source_id" + NL +
	    "  UNION" + NL +
	    "  select right.gene_source_id, right.source_id, right.project_id, genes.wdk_weight, 0 as left_match, 1 as right_match" + NL +
	    "  from genes, " + NL +
	    "  (" + getRightSql() + ") right" + NL +
	    "  where right.gene_source_id = genes.gene_source_id" + NL +
	    "  UNION" + NL +
	    "  select ta.gene_source_id, ta.source_id, genes.project_id, genes.wdk_weight, 0 as left_match, 0 as right_match" + NL +
	    "  from genes, apidbtuning.transcriptattributes ta" + NL +
	    "  where genes.gene_source_id = ta.gene_source_id) big" + NL +
	    "group by (gene_source_id, source_id, project_id, wdk_weight)";
	logger.info("TranscriptBooleanQueryInstance sql: " + NL + sql);

	return sql;	
		  
    }

   
	  
}
