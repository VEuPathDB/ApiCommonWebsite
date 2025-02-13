package org.apidb.apicommon.model.report.ai;

import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.apidb.apicommon.model.report.ai.expression.Summarizer;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.apidb.apicommon.model.TranscriptUtil;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class SingleGeneAiExpressionReporter extends AbstractReporter {    

  public enum CacheMode {
    TEST("test"),
    POPULATE("populate");
    private final String mode;
    CacheMode(String mode) {
      this.mode = mode;
    }
    public String getMode() {
      return mode;
    }
    public static CacheMode fromString(String mode) throws IllegalArgumentException {
      for (CacheMode cm : CacheMode.values()) {
        if (cm.mode.equalsIgnoreCase(mode)) {
          return cm;
        }
      }
      throw new IllegalArgumentException("Invalid CacheMode: " + mode);
    }
  }

  private CacheMode _cacheMode = CacheMode.TEST;
    
  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      if (config.has("cacheMode")) {
        _cacheMode = CacheMode.fromString(config.getString("cacheMode"));
      }
    } catch (IllegalArgumentException e) {
	    throw new ReporterConfigException("Invalid cacheMode value: " + config.getString("cacheMode"), e);
    }
    return this;
  }

  @Override
  protected void write(OutputStream out) throws IOException, WdkModelException {
    RecordClass geneRecordClass = TranscriptUtil.getGeneRecordClass(_wdkModel);
    Map<String, TableField> tableFields = geneRecordClass.getTableFieldMap();
    List<TableField> tables = List.of("ExpressionGraphs", "ExpressionGraphsDataTable").stream()
      .map(name -> tableFields.get(name))
      .collect(Collectors.toList());

    try (RecordStream recordStream = RecordStreamFactory.getRecordStream(_baseAnswer, List.of(), tables)) {
      RecordInstance singleRecord = recordStream.iterator().next();
      JSONObject expressionSummary = Summarizer.summarizeExpression(singleRecord);
      out.write(expressionSummary.toString().getBytes());
      out.flush();
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e);
    }

    
  }
  

}


