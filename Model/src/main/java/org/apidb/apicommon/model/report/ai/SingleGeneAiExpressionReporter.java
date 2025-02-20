package org.apidb.apicommon.model.report.ai;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apidb.apicommon.model.TranscriptUtil;
import org.apidb.apicommon.model.report.ai.expression.AiExpressionCache;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor;
import org.apidb.apicommon.model.report.ai.expression.Summarizer;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
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

  private static final String POPULATION_MODE_PROP_KEY = "populateIfNotPresent";

  private boolean _populateIfNotPresent;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      // assign cache mode
      _populateIfNotPresent = config.optBoolean(POPULATION_MODE_PROP_KEY, false);

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

    // get table fields needed to produce summary inputs
    Map<String, TableField> tableFields = _baseAnswer.getQuestion().getRecordClass().getTableFieldMap();
    List<TableField> tables = GeneRecordProcessor.REQUIRED_TABLE_NAMES.stream()
        .map(name -> tableFields.get(name)).collect(Collectors.toList());

    // open summary cache (manages persistence of expression data)
    AiExpressionCache cache = AiExpressionCache.getInstance(_wdkModel);

    // create summarizer (interacts with OpenAI)
    Summarizer summarizer = new Summarizer(_wdkModel);

    // open record and output streams
    try (RecordStream recordStream = RecordStreamFactory.getRecordStream(_baseAnswer, List.of(), tables);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      // write a JSON object with gene ID keys and expression summary values
      writer.write("{");
      boolean firstRecord = true;
      for (RecordInstance record : recordStream) {

        // create summary inputs
        GeneSummaryInputs summaryInputs = GeneRecordProcessor.getSummaryInputsFromRecord(record, Summarizer::getExperimentMessage);

        // fetch summary, producing if necessary and requested
        JSONObject expressionSummary = _populateIfNotPresent
            ? getSummary(summaryInputs, summarizer, cache)
            : readSummary(summaryInputs, cache);

        // join entries with commas
        if (firstRecord) firstRecord = false; else writer.write(",");

        // write JSON object
        writer.write("\"" + summaryInputs.getGeneId() + "\":" + expressionSummary.toString());

      }
    }
  }

  private JSONObject getSummary(GeneSummaryInputs summaryInputs, Summarizer summarizer, AiExpressionCache cache) {
    try {
      
    }
  }

  private JSONObject readSummary(GeneSummaryInputs summaryInputs, AiExpressionCache cache) {
    try {
      
    }
  }

}
