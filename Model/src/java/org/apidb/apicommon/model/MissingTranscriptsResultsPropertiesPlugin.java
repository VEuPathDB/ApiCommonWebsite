package org.apidb.apicommon.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.dbms.ResultList;
import org.gusdb.wdk.model.query.SqlQueryInstance;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.query.QuerySet;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.ResultProperties;
import org.apache.log4j.Logger;


/**
 * For an Transcripts AnswerValue, count how many Genes in the answer do not have all their transcripts present
 * @author Steve
 *
 */
public class MissingTranscriptsResultsPropertiesPlugin implements
		ResultProperties {

	private final static String MISSING_TRANSCRIPTS_PROP = "genesMissingTranscriptsCount";
	private final static String WDK_ID_SQL_PARAM = "WDK_ID_SQL";
	private final static String COUNT_COLUMN = "count";
	private final static String nl = System.lineSeparator();
   private static final Logger logger = Logger.getLogger(RecordClass.class);
	
	@Override
	public Map<String, String> getPropertyValues(AnswerValue answerValue) throws WdkModelException, WdkUserException {

		SqlQuery query = new SqlQuery();
	    QuerySet querySet = new QuerySet();
	    querySet.addQuery(query);
	    querySet.setCacheable(true);
		query.setName("MissingTranscriptsResultsPluginQuery");
		String sql = "select count (m.distinct_gene_source_id)" + nl +
				"from (" + nl +
				"  select ta.gene_source_id, ta.source_id" + nl +
				"  from apidbtuning.transcriptattributes ta, " + nl +
				"  ($$WDK_ID_SQL$$) idsql_1" + nl +
				"  where idsql_1.gene_source_id = ta.gene_source_id" + nl +
				"  MINUS" + nl +
				"  select gene_source_id, source_id" + nl +
				"  from ($$WDK_ID_SQL$$) idsql_2" + nl +
		    ") m" ;
		query.setSql(sql);
		query.resolveReferences(answerValue.getQuestion().getRecordClass().getWdkModel());
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put(WDK_ID_SQL_PARAM, answerValue.getIdSql());
		SqlQueryInstance queryInstance;
		try {
			queryInstance = query.makeInstance(answerValue.getUser(), params, true, 0,
					new LinkedHashMap<String, String>());
		}
		catch (WdkUserException ex) {
			throw new WdkModelException(ex);
		}

		logger.info("================================== sql:" + queryInstance.getSql());
		ResultList results = queryInstance.getResults();
		results.next();
		Integer count = (Integer)results.get(COUNT_COLUMN);
		RecordClass recordClass = answerValue.getQuestion().getRecordClass();
		if (results.next()) throw new WdkModelException("Record class '"  + recordClass.getName() + "' has an SqlResultSizePlugin whose SQL returns more than one row.");

		return null;
	}

	@Override
	public void validatePropertyNames(List<String> propNames) throws WdkModelException {
		if (propNames.size() != 1 || !propNames.get(0).equals(MISSING_TRANSCRIPTS_PROP)) throw new WdkModelException("MissingTranscriptsResultsPropertiesPlugin called with incorrect property name '" + propNames.get(0) + "'");
	}


}
