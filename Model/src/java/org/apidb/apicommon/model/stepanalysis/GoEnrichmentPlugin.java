package org.apidb.apicommon.model.stepanalysis;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.StatusLogger;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.sql.DataSource;

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LongRunningTestPlugin.class);

  @Override
  public List<String> validateFormParams(Map<String, String[]> formParams) {
    List<String> errors = new ArrayList<String>();

    // validate pValueCutoff
    if (!formParams.containsKey("pValueCutoff")) errors.add("Missing required parameter 'pValueCutoff'");
    try {
      float pValueCutoff = Float.parseFloat(formParams.get("pValueCutoff")[0]);
      if (pValueCutoff <= 0 || pValueCutoff > 1) throw new NumberFormatException();
    } catch (NumberFormatException e) {
      errors.add("Parameter 'pValueCutoff' must be a number between greater than 0 and less than or equal to 1");
    }
    
    // validate annotation sources
    if (!formParams.containsKey("goAssociationsSources")) errors.add("Missing required param 'goAssociationsSources'");
    return errors;
  }

  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get("pValueCutoff")[0];
    String sourcesStr = "";
    for (String src : params.get("goAssociationsSources")) {
      sourcesStr += src + ",";
    }
    sourcesStr = sourcesStr.substring(0, sourcesStr.length()-1);

    String sql = "SELECT count(distinct ga.taxon_id) as cnt" + System.lineSeparator() +
      "FROM ApidbTuning.GeneAttributes ga,"  + System.lineSeparator() +
      "(" + idSql + ") r"  + System.lineSeparator() +
      "where ga.source_id = r.source_id";

    DataSource ds = getWdkModel().getAppDb().getDataSource();
    BasicResultSetHandler handler = new BasicResultSetHandler();
    new SQLRunner(ds, sql).executeQuery(handler);
    List<Map<String, Object>> result = handler.getResults();
    Integer count = (Integer)(result.get(0).get("cnt"));

    return new String[]{ "apiGoEnrichment", idSql, pValueCutoff, getStorageDirectory() + "/result", wdkModel.getProjectId(), sourcesStr };
  }

  public Object getResultViewModel() throws WdkModelException {
    return null;
  }

}
