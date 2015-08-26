package org.apidb.apicommon.model.filter;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.filter.FilterSummary;
import org.gusdb.wdk.model.filter.StepFilter;
import org.json.JSONObject;

public class RepresentativeTranscriptFilter extends StepFilter {

  private static final Logger LOG = Logger.getLogger(RepresentativeTranscriptFilter.class);

  /**
   * The following String value is used in a variety of places.  Do not change
   *      unless you know what you're doing.  Here's where it is used: <br/>
   * <ol>
   *   <li>Name of the representative-transcript-only view filter (map key in Java code)</li>
   *   <li>Name of the JSON property in the database within the view-only filter property value object</li>
   *   <li>Name of the request-scope JSP-EL variable for use by the transcript summary view JSP</li>
   *   <li>Name of the JSON property in the POST service request body used to change the value</li>
   * </ol>
   */
  public static final String FILTER_NAME = "representativeTranscriptOnly";

  private static final String ORIG_SQL_PARAM = "%%originalSql%%";

  private static final String FILTER_SQL =
      "WITH inputSql as (" + ORIG_SQL_PARAM + ") " +
      "SELECT * FROM inputSql " +
      "WHERE SOURCE_ID IN ( " +
      "  SELECT MIN(subq_.SOURCE_ID) FROM inputSql subq_ " +
      "  GROUP BY subq_.GENE_SOURCE_ID " +
      ")";

  public RepresentativeTranscriptFilter() {
    super(FILTER_NAME);
  }

  @Override
  public String getDisplayValue(AnswerValue answer, JSONObject jsValue) throws WdkModelException,
      WdkUserException {
    return "Shows only a representative transcript for each gene.";
  }

  @Override
  public FilterSummary getSummary(AnswerValue answer, String idSql) throws WdkModelException,
      WdkUserException {
    throw new UnsupportedOperationException("This filter does not provide a FilterSummary");
  }

  @Override
  public String getSql(AnswerValue answer, String idSql, JSONObject jsValue) throws WdkModelException,
      WdkUserException {
    LOG.info("Applying Representative Transcript Filter to SQL: " + idSql);
    return FILTER_SQL.replace(ORIG_SQL_PARAM, idSql);
  }

  @Override
  public boolean defaultValueEquals(JSONObject value) throws WdkModelException {
    return false;
  }

}
