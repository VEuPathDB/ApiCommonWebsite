package org.apidb.apicommon.model.stepanalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.xml.NamedValue;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractSimpleProcessAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;

public class GoEnrichmentPlugin extends AbstractSimpleProcessAnalyzer {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(LongRunningTestPlugin.class);

  public static final String PVALUE_PARAM_KEY = "pValueCutoff";
  public static final String GO_ASSOC_SRC_PARAM_KEY = "goAssociationsSources";

  public static final List<NamedValue> ASSOC_SRC_OPTIONS = new ListBuilder<NamedValue>()
      .add(new NamedValue("GeneDB","GeneDB"))
      .add(new NamedValue("InterproScan","InterproScan"))
      .toList();
  
  public static class FormViewModel {
    public List<NamedValue> getSourceOptions() {
      return ASSOC_SRC_OPTIONS;
    }
  }
  
  @Override
  public Map<String,String> validateFormParams(Map<String, String[]> formParams) {
    return validateParams(formParams);
  }
  
  public static Map<String,String> validateParams(Map<String, String[]> formParams) {
    Map<String,String> errors = new HashMap<String,String>();

    // validate pValueCutoff
    if (!formParams.containsKey(PVALUE_PARAM_KEY)) {
      errors.put(PVALUE_PARAM_KEY, "Missing required parameter.");
    }
    else {
      try {
        float pValueCutoff = Float.parseFloat(formParams.get(PVALUE_PARAM_KEY)[0]);
        if (pValueCutoff <= 0 || pValueCutoff > 1) throw new NumberFormatException();
      }
      catch (NumberFormatException e) {
        errors.put(PVALUE_PARAM_KEY, "Must be a number between greater than 0 and less than or equal to 1.");
      }
    }
    
    // validate annotation sources
    String [] sources = formParams.get(GO_ASSOC_SRC_PARAM_KEY);
    if (sources == null || sources.length == 0) {
      errors.put(GO_ASSOC_SRC_PARAM_KEY, "Missing required parameter.");
    }
  
    return errors;
  }

  @Override
  protected String[] getCommand(AnswerValue answerValue) throws WdkModelException {

    WdkModel wdkModel = answerValue.getQuestion().getWdkModel();
    String idSql = answerValue.getIdSql();

    Map<String,String[]> params = getFormParams();

    String pValueCutoff = params.get(PVALUE_PARAM_KEY)[0];
    String sourcesStr = FormatUtil.join(params.get(GO_ASSOC_SRC_PARAM_KEY), ",");

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

  @Override
  public Object getFormViewModel() throws WdkModelException {
    return new FormViewModel();
  }
  
  @Override
  public Object getResultViewModel() throws WdkModelException {
    return null;
  }

}
