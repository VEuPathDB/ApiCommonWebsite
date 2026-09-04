package org.apidb.apicommon.model.report.ai.expression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.ExperimentInputs;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingModel;

public abstract class Summarizer {

  protected static final int MAX_RESPONSE_TOKENS = 10000;

  public static final EmbeddingModel EMBEDDING_MODEL = EmbeddingModel.TEXT_EMBEDDING_3_SMALL;
  private static final int EMBEDDING_DIMENSIONS = 512;
  private static final int EMBEDDING_DECIMAL_PLACES = 4;

  private static final int MAX_MALFORMED_RESPONSE_RETRIES = 3;
  private static final String OPENAI_API_KEY_PROP_NAME = "OPENAI_API_KEY";

  protected static final String SYSTEM_MESSAGE = "You are a bioinformatician working for VEuPathDB.org. You are an expert at providing biologist-friendly summaries of transcriptomic data";

  // Platform-agnostic JSON Schema documents for structured responses. Plain
  // Map/List literals rather than either provider's SDK types: both
  // com.openai.core.JsonValue.from(Object) and com.anthropic.core.JsonValue.from(Object)
  // recursively serialize arbitrary Map/List/primitive graphs, so each Summarizer
  // subclass wraps this same literal in its own SDK's schema type at request time.
  protected static final Map<String, Object> experimentResponseSchema = Map.of(
      "type", "object",
      "properties", Map.of(
          "one_sentence_summary", Map.of("type", "string", "description",
              "One-sentence, user-facing summary of how this gene is expressed in this experiment. " +
              "State whether expression is up- or down-regulated (and by how much) relative to the " +
              "experimental conditions tested; do not describe the experiment itself. Wrap species " +
              "names in <i> tags."),
          "biological_importance", Map.of("type", "integer", "minimum", 0, "maximum", 5, "description",
              "Estimated biological importance of this expression profile relative to other " +
              "experiments, on an integer scale from 0 (lowest, no differential expression) to 5 " +
              "(highest, marked differential expression)."),
          "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 5, "description",
              "Confidence in the biological_importance estimate, on the same 0 (lowest) to 5 " +
              "(highest) integer scale."),
          "experiment_keywords", Map.of("type", "array", "items", Map.of("type", "string"), "description",
              "General experiment-based keywords giving additional context to the gene-based " +
              "expression summary, e.g. \"tachyzoite\", \"RNA-Seq\", \"oocyst sporulation\", " +
              "\"host cell infection\". Not shown to users directly."),
          "notes", Map.of("type", "string", "description",
              "Optional caveats, peculiarities, or additional context that may aid interpretation " +
              "and further analysis. Not shown to users directly; passed to a second AI " +
              "summarization step.")
      ),
      "required", List.of(
          "one_sentence_summary",
          "biological_importance",
          "confidence",
          "experiment_keywords",
          "notes"
      ),
      "additionalProperties", false
  );

  protected static final Map<String, Object> finalResponseSchema = Map.of(
      "type", "object",
      "properties", Map.of(
          "headline", Map.of("type", "string", "description",
              "Short, specific headline reflecting this gene's expression pattern, in sentence " +
              "case (capitalize only the first word and proper nouns). Must NOT include generic " +
              "phrases like \"comprehensive insights into\" or the word \"gene\"."),
          "one_paragraph_summary", Map.of("type", "string", "description",
              "~100-word summary of the gene's expression, structured using <strong>, <ul>, and " +
              "<li> tags with no attributes. May briefly speculate on the gene's potential " +
              "function, but only if justified by the data. Wrap species names in <i> tags."),
          "topics", Map.of("type", "array", "minItems", 1, "description",
              "Groups the per-experiment summaries (identified by dataset_id, from the input) " +
              "with biological_importance > 3 and confidence > 3 into sections by topic. These " +
              "are displayed to users.", "items", Map.of(
              "type", "object",
              "required", List.of("headline", "one_sentence_summary", "dataset_ids"),
              "properties", Map.of(
                  "headline", Map.of("type", "string", "description",
                      "Headline summarizing the key experimental results within this topic."),
                  "one_sentence_summary", Map.of("type", "string", "description",
                      "Concise one-sentence summary of this topic's experimental results. Wrap " +
                      "species names in <i> tags."),
                  "dataset_ids", Map.of("type", "array", "items", Map.of("type", "string"), "description",
                      "dataset_id values (from the input) of the experiments grouped into this topic.")
              ),
              "additionalProperties", false
          ))
      ),
      "required", List.of(
          "headline",
          "one_paragraph_summary",
          "topics"
      ),
      "additionalProperties", false
  );

  protected final DailyCostMonitor _costMonitor;
  private final OpenAIClientAsync _embeddingClient;
  protected final boolean _makeTopicEmbeddings;

  private static final Logger LOG = Logger.getLogger(Summarizer.class);

  public Summarizer(WdkModel wdkModel, DailyCostMonitor costMonitor, boolean makeTopicEmbeddings) throws WdkModelException {
    _costMonitor = costMonitor;
    _makeTopicEmbeddings = makeTopicEmbeddings;

    // Only create embedding client if we need to make topic embeddings
    if (makeTopicEmbeddings) {
      String apiKey = wdkModel.getProperties().get(OPENAI_API_KEY_PROP_NAME);
      if (apiKey == null) {
        throw new WdkModelException("WDK property '" + OPENAI_API_KEY_PROP_NAME + "' has not been set.");
      }

      _embeddingClient = OpenAIOkHttpClientAsync.builder()
          .apiKey(apiKey)
          .maxRetries(32)  // Handle 429 errors
          .build();
    } else {
      _embeddingClient = null;
    }
  }

  private CompletableFuture<List<Double>> getEmbedding(String text) {
    // Safety check: ensure embedding client was initialized
    if (_embeddingClient == null) {
      LOG.error("Attempted to generate embedding but embedding client was not initialized (makeTopicEmbeddings=false)");
      return CompletableFuture.completedFuture(List.of());
    }

    EmbeddingCreateParams request = EmbeddingCreateParams.builder()
        .model(EMBEDDING_MODEL)
        .input(text)
        .dimensions(EMBEDDING_DIMENSIONS)
        .build();

    return _embeddingClient.embeddings().create(request).thenApply(response -> {
      // Update cost monitor - convert embedding usage to TokenUsage
      com.openai.models.embeddings.CreateEmbeddingResponse.Usage embeddingUsage = response.usage();
      TokenUsage tokenUsage = TokenUsage.builder()
          .embeddingTokens(embeddingUsage.totalTokens())
          .build();
      _costMonitor.updateCost(tokenUsage);

      // Extract embedding vector from first result (convert Float to Double)
      List<Float> rawEmbedding = response.data().get(0).embedding();

      // Round to specified decimal places
      double scale = Math.pow(10, EMBEDDING_DECIMAL_PLACES);
      return rawEmbedding.stream()
          .map(val -> Math.round(val.doubleValue() * scale) / scale)
          .collect(Collectors.toList());
    }).exceptionally(e -> {
      LOG.error("Failed to generate embedding: " + e.getMessage(), e);
      return List.of(); // Return empty list on error
    });
  }

  /**
   * Retries an operation with exponential backoff if it fails with a retriable error.
   *
   * @param <T> the return type of the operation
   * @param operation supplier that produces the CompletableFuture to execute
   * @param shouldRetry predicate to determine if an exception should trigger a retry
   * @param operationDescription description for logging purposes
   * @return CompletableFuture with the result of the operation
   */
  protected <T> CompletableFuture<T> retryOnOverload(
      java.util.function.Supplier<CompletableFuture<T>> operation,
      java.util.function.Predicate<Throwable> shouldRetry,
      String operationDescription) {

    final int maxRetries = 3;
    final long[] backoffDelaysMs = {1000, 2000, 4000}; // 1s, 2s, 4s

    return retryWithBackoff(operation, shouldRetry, operationDescription, 0, maxRetries, backoffDelaysMs);
  }

  private <T> CompletableFuture<T> retryWithBackoff(
      java.util.function.Supplier<CompletableFuture<T>> operation,
      java.util.function.Predicate<Throwable> shouldRetry,
      String operationDescription,
      int attemptNumber,
      int maxRetries,
      long[] backoffDelaysMs) {

    CompletableFuture<T> result = new CompletableFuture<>();

    operation.get().whenComplete((value, throwable) -> {
      if (throwable == null) {
        // Success case
        result.complete(value);
      } else {
        // Error case - unwrap CompletionException to get the actual cause
        Throwable actualCause = throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null
            ? throwable.getCause()
            : throwable;

        // Check if we should retry this exception and haven't exceeded max retries
        if (shouldRetry.test(actualCause) && attemptNumber < maxRetries) {
          long delayMs = backoffDelaysMs[attemptNumber];
          LOG.warn(String.format(
              "Retrying %s after error (attempt %d/%d, waiting %dms): %s",
              operationDescription, attemptNumber + 1, maxRetries, delayMs, actualCause.getMessage()));

          // Schedule retry after delay
          new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
              retryWithBackoff(operation, shouldRetry, operationDescription, attemptNumber + 1, maxRetries, backoffDelaysMs)
                  .whenComplete((retryValue, retryError) -> {
                    if (retryError != null) {
                      result.completeExceptionally(retryError);
                    } else {
                      result.complete(retryValue);
                    }
                  });
            }
          }, delayMs);
        } else {
          // No more retries or non-retriable exception
          if (attemptNumber >= maxRetries) {
            LOG.error(String.format("Failed %s after %d retries: %s", operationDescription, maxRetries, actualCause.getMessage()));
          }
          result.completeExceptionally(throwable);
        }
      }
    });

    return result;
  }

  public static String getExperimentMessage(JSONObject experiment) {

    // Possible TO DO: AI EDIT DESCRIPTION
    // Before sending the experiment+data to the AI, ask the AI to edit the `description` field
    // as follows: (This should be cached by dataset_id only and would be called once per organism
    // and would reduce tokens and "cognitive load" a little bit for the next step.)
    //
    // "Edit the following text to so that it **only** describes the experimental design of the
    // transcriptomics part of the study. Do not mention the results of any bioinformatics analyses performed,
    // especially not any genes or groups of genes and their expression behaviour."
    //
    // We would then be able to remove the "Ignore all discussion of individual or groups of genes in the
    // experiment `description`, as this is irrelevant to the gene you are summarising." from the prompt
    // below.

    // We don't need to send dataset_id to the AI but it's useful to have it
    // in the response for phase two
    JSONObject experimentForAI = new JSONObject(experiment.toString(2)); // clone
    experimentForAI.remove("dataset_id");

    return
        "The JSON below contains expression data for a single gene within a specific experiment, along with relevant experimental and bioinformatics metadata:\n\n" +
        String.format("```json\n%s\n```\n\n", JsonUtil.serialize(experimentForAI)) +
        "**Task**: In one sentence, summarize how this gene is expressed in the given experiment. Do not describe the experiment itself—focus on whether the gene is, or is not, substantially and/or significantly upregulated or downregulated with respect to the experimental conditions tested. Take extreme care to assert the correct directionality of the response, especially in experiments with only one or two samples. Additionally, estimate the biological importance of this profile relative to other experiments, your confidence in that estimate, and provide some general experiment-based keywords (see the response schema's field descriptions for details on each).\n" +
        "**Purpose**: The one-sentence summary will be displayed to users in tabular form on our gene-page. Please wrap user-facing species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers. The notes, scores and keywords will not be shown to users, but will be passed along with the summary to a second AI summarisation step that synthesizes insights from multiple experiments.\n" +
        "**Further guidance**: The `y_axis` field describes the `value` field in the `data` array, which is the primary expression level datum. Note that standard error statistics are only available when biological replicates were performed. However, percentile-normalized values can also guide your assessment of importance. If this is a time-series experiment, consider if it is cyclical and assess periodicity as appropriate. Ignore all discussion of individual or groups of genes in the experiment `description`, as this is irrelevant to the gene you are summarising. For RNA-Seq experiments, be aware that if `paralog_number` is high, interpretation may be tricky (consider both unique and non-unique counts if available).";
  }

  public CompletableFuture<JSONObject> describeExperiment(ExperimentInputs experimentInputs) {

    String prompt = getExperimentMessage(experimentInputs.getExperimentData());

    return getValidatedAiResponse("dataset " + experimentInputs.getDatasetId(), prompt, experimentResponseSchema,
        Set.of("one_sentence_summary"), json -> {
      // add some fields to the result to aid the final summarization
      return json
        .put("dataset_id", experimentInputs.getDatasetId())
        .put("assay_type", experimentInputs.getAssayType())
        .put("experiment_name", experimentInputs.getExperimentName());
    });
  }

  public static String getFinalSummaryMessage(List<JSONObject> experiments) {
    return "Below are AI-generated summaries of one gene's behavior in all the transcriptomics experiments available in VEuPathDB, provided in JSON format:\n\n" +
        String.format("```json\n%s\n```\n\n", new JSONArray(experiments).toString(2)) +
        "Generate a one-paragraph summary and headline describing the gene's expression overall, and group the per-experiment summaries into topics (see the response schema's field descriptions for formatting requirements and the topic-grouping threshold). In all generated text, use clear, precise scientific language accessible to non-native English speakers.";
  }
  
  public JSONObject summarizeExperiments(String geneId, List<JSONObject> experiments) {

    String prompt = getFinalSummaryMessage(experiments);

    return getValidatedAiResponse("summary for gene " + geneId, prompt, finalResponseSchema,
      Set.of("headline", "one_paragraph_summary"), json ->
      json  // Return json as-is; consolidateSummary will be called separately
    ).thenCompose(json ->
      // quality control (remove bad `dataset_id`s) and add 'Others' section for any experiments not listed by AI
      consolidateSummary(json, experiments)
    ).join();
  }

  private CompletableFuture<JSONObject> consolidateSummary(JSONObject summaryResponse,
      List<JSONObject> individualResults) {
    // Gather all dataset IDs from individualResults and map them to summaries.
    // Preserving the order of individualResults.
    Map<String, JSONObject> datasetSummaries = new LinkedHashMap<>();
    for (JSONObject result : individualResults) {
      datasetSummaries.put(result.getString("dataset_id"), result);
    }

    Set<String> seenDatasetIds = new LinkedHashSet<>();
    List<JSONObject> deduplicatedTopicsList = new java.util.ArrayList<>();
    List<CompletableFuture<Void>> embeddingFutures = new java.util.ArrayList<>();
    JSONArray topics = summaryResponse.getJSONArray("topics");

    for (int i = 0; i < topics.length(); i++) {
      JSONObject topic = topics.getJSONObject(i);
      JSONArray datasetIds = topic.getJSONArray("dataset_ids");
      JSONArray summaries = new JSONArray();

      for (int j = 0; j < datasetIds.length(); j++) {
        String id = datasetIds.getString(j);

        // Warn and skip if the id doesn't exist
        if (!datasetSummaries.containsKey(id)) {
          System.out.println(
              "WARNING: dataset_id '" + id + "' does not exist. Excluding from final output.");
          continue;
        }
        // Skip if we've seen it
        if (seenDatasetIds.contains(id))
          continue;

        seenDatasetIds.add(id);
        summaries.put(datasetSummaries.get(id));
      }

      // Update topic with mapped summaries and remove dataset_ids key
      // but only if it's a non-empty topic (can happen with bad dataset_ids, see above)
      if (summaries.length() > 0) {
        topic.put("summaries", summaries);
        topic.remove("dataset_ids");
        deduplicatedTopicsList.add(topic);

        // Generate embedding for non-"Other" topics (if enabled)
        if (_makeTopicEmbeddings) {
          String headline = topic.optString("headline", "");
          if (!headline.equals("Other")) {
            String embeddingText = headline + "\n\n" + topic.optString("one_sentence_summary", "");
            CompletableFuture<Void> embeddingFuture = getEmbedding(embeddingText).thenAccept(embedding -> {
              if (!embedding.isEmpty()) {
                topic.put("embedding_vector", embedding);
              }
            });
            embeddingFutures.add(embeddingFuture);
          }
        }
      }
    }

    // Find missing dataset IDs (preserve dataset order)
    Set<String> missingDatasetIds = new LinkedHashSet<>(datasetSummaries.keySet());
    missingDatasetIds.removeAll(seenDatasetIds);

    // If there are missing IDs, add an "Others" topic
    if (!missingDatasetIds.isEmpty()) {
      JSONArray otherSummaries = new JSONArray();
      for (String id : missingDatasetIds) {
        otherSummaries.put(datasetSummaries.get(id));
      }

      JSONObject otherTopic = new JSONObject();
      otherTopic.put("headline", "Other");
      otherTopic.put("one_sentence_summary",
          "The AI ordered these experiments by biological importance but did not group them into topics.");
      otherTopic.put("summaries", otherSummaries);
      deduplicatedTopicsList.add(otherTopic);
      // Note: no embedding for "Other" topic
    }

    // Wait for all embeddings to complete, then create final summary
    return CompletableFuture.allOf(embeddingFutures.toArray(new CompletableFuture[0]))
        .thenApply(v -> {
          // Convert deduplicated topics list back to JSONArray
          JSONArray deduplicatedTopics = new JSONArray();
          for (JSONObject topic : deduplicatedTopicsList) {
            deduplicatedTopics.put(topic);
          }

          // Create final deduplicated summary
          JSONObject finalSummary = new JSONObject(summaryResponse.toString());
          finalSummary.put("topics", deduplicatedTopics);
          return finalSummary;
        });
  }


  // Sentinel word(s) Claude has been observed writing INTO a primary content field itself
  // when it has nothing real to say - not just leaving the field blank. Seen in
  // "notes":"placeholder" (a secondary field, already excluded by only checking
  // primaryContentFields), "one_sentence_summary":"placeholder" (a primary field, where a
  // blank-only check misses it entirely), and - the reason this is a substring check, not
  // an exact match - "empty_response_reason":"Malformed JSON keys placeholder" (a longer,
  // still-garbled phrase that merely contains the word). We tried offering an explicit
  // empty_response_reason field for the model to explain itself; it just wrote the same
  // filler word into that field too, so it was removed rather than kept as a channel that
  // never carried a real explanation. "placeholder" isn't a real biology term, so a
  // substring match is very unlikely to collide with genuine content.
  private static final Set<String> DEGENERATE_FIELD_VALUES = Set.of("placeholder");

  private static boolean containsDegenerateValue(String text) {
    String lower = text.toLowerCase();
    return DEGENERATE_FIELD_VALUES.stream().anyMatch(lower::contains);
  }

  // Checks only the caller-designated user-facing field(s), not every field in the
  // response: a degenerate reply can still have non-blank junk in a secondary field
  // (observed: {"one_sentence_summary":"","notes":"placeholder",...} - "notes" alone
  // would make an "are all fields blank" check miss this), so the real signal is
  // specifically whether the field(s) actually shown to users are empty or sentinel junk.
  private static boolean looksEmpty(JSONObject json, Set<String> primaryContentFields) {
    for (String field : primaryContentFields) {
      Object value = json.opt(field);
      if (value instanceof String str) {
        String trimmed = str.trim();
        if (!trimmed.isEmpty() && !containsDegenerateValue(trimmed)) {
          return false;
        }
      }
      if (value instanceof JSONArray arr && arr.length() > 0) {
        return false;
      }
    }
    return true;
  }

  protected abstract CompletableFuture<String> callApiForJson(String prompt, Map<String, Object> schema);

  protected abstract void updateCostMonitor(Object apiResponse);

  private CompletableFuture<JSONObject> getValidatedAiResponse(
      String operationDescription,
      String prompt,
      Map<String, Object> schema,
      Set<String> primaryContentFields,
      Function<JSONObject,JSONObject> createFinalJson
  ) {
    return callApiForJson(prompt, schema).thenApply(jsonString -> {
      int attempts = 1;

      while (true) {
        try {
          // convert to JSON object
          JSONObject jsonObject = new JSONObject(jsonString);

          // Some AI responses are syntactically valid JSON matching the schema, but have
          // nothing to say in the field(s) actually shown to users (e.g.
          // {"one_sentence_summary":"","biological_importance":0,"confidence":0,
          // "experiment_keywords":[],"notes":"placeholder"}) - observed from Claude with
          // stop_reason=end_turn, so nothing else catches it. Treat the same as a parse
          // failure so it gets logged and retried rather than silently cached. (We tried an
          // empty_response_reason field so the model could explain itself instead of going
          // blank; it just wrote the same filler word into that field too, so it was removed.)
          if (looksEmpty(jsonObject, primaryContentFields)) {
            throw new JSONException("AI response is syntactically valid but " + primaryContentFields +
                " is blank/empty: " + jsonString);
          }

          // convert AI response JSON into final JSON we want to store
          return createFinalJson.apply(jsonObject);
        }
        catch (JSONException e) {
          LOG.warn("Malformed or empty JSON from AI (attempt " + attempts + ") for " + operationDescription +
              ": " + e.getMessage() + ". Retrying...");

          // Give up only once we've validated MAX_MALFORMED_RESPONSE_RETRIES responses and all
          // failed - don't fetch one more that would then go unchecked. (Previously the retry
          // was requested unconditionally here, before the loop condition was checked, so on
          // the last iteration a 4th response was fetched - and paid for - but the loop exited
          // before ever validating it, silently discarding it (even when, as observed, it was
          // a perfectly good answer) and reporting failure using its unvalidated content.)
          if (attempts >= MAX_MALFORMED_RESPONSE_RETRIES) {
            String message = "Failed to parse JSON after " + MAX_MALFORMED_RESPONSE_RETRIES + " attempts for " +
                operationDescription + ". Raw response: " + jsonString;
            LOG.error(message, e);
            throw new RuntimeException(message, e);
          }

          // Re-request from AI
          jsonString = callApiForJson(prompt, schema).join();
          attempts++;
        }
      }
    });
  }
}
