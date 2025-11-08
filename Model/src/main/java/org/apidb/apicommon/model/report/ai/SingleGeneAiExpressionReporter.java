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
import org.apidb.apicommon.model.report.ai.expression.DailyCostMonitor;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
import org.apidb.apicommon.model.report.ai.expression.ClaudeSummarizer;
//import org.apidb.apicommon.model.report.ai.expression.OpenAISummarizer;
import org.apidb.apicommon.model.report.ai.expression.Summarizer;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkServiceTemporarilyUnavailableException;
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

/**
 * Reporter that generates AI-powered gene expression summaries using LLM models.
 *
 * <p>This reporter analyzes expression data across multiple experiments for a single gene
 * and generates natural language summaries of expression patterns and biological significance.
 * Results are cached to minimize API costs and response times.</p>
 *
 * <h3>Configuration (JSON request payload)</h3>
 * <pre>
 * {
 *   "populateIfNotPresent": true|false,  // If true, generate summary if not cached (default: false)
 *   "makeTopicEmbeddings": true|false    // If true, generate embedding vectors for topics (default: false)
 * }
 * </pre>
 *
 * <h3>Cache Invalidation Warning</h3>
 * <p><strong>IMPORTANT:</strong> Changing the {@code makeTopicEmbeddings} setting will invalidate
 * the entire cache for all genes, as this value is included in the cache digest. To avoid costly
 * cache regeneration, choose a setting and stick with it across requests. Only change this value
 * when you intentionally want to regenerate all summaries with or without embeddings.</p>
 *
 * <h3>Model Configuration</h3>
 * <p>The AI model and embedding model are hardcoded in the summarizer implementations
 * ({@link ClaudeSummarizer}, {@link org.apidb.apicommon.model.report.ai.expression.OpenAISummarizer}).
 * Changing models will also invalidate the cache.</p>
 */
public class SingleGeneAiExpressionReporter extends AbstractReporter {

  private static final int MAX_RESULT_SIZE = 1; // one gene at a time for now

  private static final String POPULATION_MODE_PROP_KEY = "populateIfNotPresent";
  private static final String AI_MAX_CONCURRENT_REQUESTS_PROP_KEY = "AI_MAX_CONCURRENT_REQUESTS";
  private static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 10;
  private static final String MAKE_TOPIC_EMBEDDINGS_PROP_KEY = "makeTopicEmbeddings";

  private boolean _populateIfNotPresent;
  private int _maxConcurrentRequests;
  private boolean _makeTopicEmbeddings;
  private DailyCostMonitor _costMonitor;

  @Override
  public Reporter configure(JSONObject config) throws ReporterConfigException, WdkModelException {
    try {
      // assign cache mode
      _populateIfNotPresent = config.optBoolean(POPULATION_MODE_PROP_KEY, false);

      // assign topic embeddings flag
      _makeTopicEmbeddings = config.optBoolean(MAKE_TOPIC_EMBEDDINGS_PROP_KEY, false);

      // read max concurrent requests from model properties or use default
      String maxConcurrentRequestsStr = _wdkModel.getProperties().get(AI_MAX_CONCURRENT_REQUESTS_PROP_KEY);
      _maxConcurrentRequests = maxConcurrentRequestsStr != null
          ? Integer.parseInt(maxConcurrentRequestsStr)
          : DEFAULT_MAX_CONCURRENT_REQUESTS;

      // instantiate cost monitor
      _costMonitor = new DailyCostMonitor(_wdkModel);

      // check model config; this should only be assigned to genes
      RecordClass geneRecordClass = TranscriptUtil.getGeneRecordClass(_wdkModel);
      if (_baseAnswer.getQuestion().getRecordClass() != geneRecordClass) {
        throw new WdkModelException(SingleGeneAiExpressionReporter.class.getName() +
            " should only be assigned to " + geneRecordClass.getFullName());
      }

      // check result size; limit to small results due to AI API cost
      if (_baseAnswer.getResultSizeFactory().getResultSize() > MAX_RESULT_SIZE) {
        throw new ReporterConfigException("This reporter cannot be called with results of size greater than " + MAX_RESULT_SIZE);
      }

      // if we might do some work, check daily cost and throw before write() is called so caller gets the proper HTTP response code
      if (_populateIfNotPresent && _costMonitor.isCostExceeded()) {
        throw new WdkServiceTemporarilyUnavailableException("Daily token limit for AI expression summarization reached.");
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

    // create summarizer (interacts with Claude)
    ClaudeSummarizer summarizer = new ClaudeSummarizer(_wdkModel, _costMonitor, _makeTopicEmbeddings);
    // or alternatively use OpenAI (with the appropriate import)
    // OpenAISummarizer summarizer = new OpenAISummarizer(_wdkModel, _costMonitor, _makeTopicEmbeddings);
    
    // open record and output streams
    try (RecordStream recordStream = RecordStreamFactory.getRecordStream(_baseAnswer, List.of(), tables);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      // write a JSON object with gene ID keys and expression summary values
      writer.write("{");
      boolean firstRecord = true;
      for (RecordInstance record : recordStream) {

        // create summary inputs
        GeneSummaryInputs summaryInputs =
            GeneRecordProcessor.getSummaryInputsFromRecord(record, ClaudeSummarizer.CLAUDE_MODEL.toString(),
                Summarizer.EMBEDDING_MODEL.asString(), _makeTopicEmbeddings,
                Summarizer::getExperimentMessage, Summarizer::getFinalSummaryMessage);

        // fetch summary, producing if necessary and requested
        JSONObject expressionSummary = _populateIfNotPresent
            ? cache.populateSummary(summaryInputs, summarizer::describeExperiment, summarizer::summarizeExperiments, _maxConcurrentRequests)
            : cache.readSummary(summaryInputs);

        // join entries with commas
        if (firstRecord) firstRecord = false; else writer.write(",");

        // write JSON object property, keyed by gene ID
        writer.write("\"" + summaryInputs.getGeneId() + "\":" + expressionSummary.toString());

      }
      writer.write("}");
    }
  }
}
