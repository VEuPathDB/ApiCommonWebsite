package org.apidb.apicommon.model.report.ai;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor;
import org.apidb.apicommon.model.report.ai.expression.Summarizer;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableField;
import org.gusdb.wdk.model.report.AbstractReporter;
import org.gusdb.wdk.model.report.Reporter;
import org.gusdb.wdk.model.report.ReporterConfigException;
import org.json.JSONException;
import org.json.JSONObject;

public class SingleGeneAiExpressionReporter extends AbstractReporter {

  private static final int MAX_RESULT_SIZE = 1; // one gene at a time for now

  private CacheMode _cacheMode = CacheMode.TEST;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      // assign cache mode
      if (config.has("cacheMode")) {
        _cacheMode = CacheMode.valueOf(config.getString("cacheMode").toUpperCase());
      }

      // check model config; this should only be assigned to genes
      RecordClass geneRecordClass = TranscriptUtil.getGeneRecordClass(_wdkModel);
      if (_baseAnswer.getQuestion().getRecordClass() != geneRecordClass) {
        throw new WdkModelException(SingleGeneAiExpressionReporter.class.getName() +
            " should only be assigned to " + geneRecordClass.getFullName());
      }

      // check result size; limit to small results due to OpenAI cost
      if (_baseAnswer.getResultSizeFactory().getResultSize() > MAX_RESULT_SIZE) {
        throw new ReporterConfigException("This reporter cannot be called with results of size greater than " + MAX_RESULT_SIZE);
      }
    }
    catch (JSONException | IllegalArgumentException e) {
      throw new ReporterConfigException("Invalid cacheMode value: " + config.get("cacheMode"), e);
    }
    return this;
  }

  @Override
  protected void write(OutputStream out) throws IOException, WdkModelException {

    Map<String, TableField> tableFields = _baseAnswer.getQuestion().getRecordClass().getTableFieldMap();
    List<TableField> tables = GeneRecordProcessor.REQUIRED_TABLE_NAMES.stream()
        .map(name -> tableFields.get(name)).collect(Collectors.toList());

    try (RecordStream recordStream = RecordStreamFactory.getRecordStream(_baseAnswer, List.of(), tables);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
      for (RecordInstance record : recordStream) {
        JSONObject expressionSummary = Summarizer.summarizeExpression(record, _cacheMode);
        writer.write(expressionSummary.toString());
      }
    }
    catch (WdkUserException e) {
      throw new WdkModelException(e);
    }
  }

}
