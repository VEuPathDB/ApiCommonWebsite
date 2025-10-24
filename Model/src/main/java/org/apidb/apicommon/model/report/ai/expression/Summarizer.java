package org.apidb.apicommon.model.report.ai.expression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema.Schema;

public class Summarizer {

  // provide exact model number for semi-reproducibility
  public static final ChatModel OPENAI_CHAT_MODEL = ChatModel.GPT_4O_2024_11_20; // GPT_4O_2024_08_06;

  private static final int MAX_RESPONSE_TOKENS = 10000;

  private static final int MAX_MALFORMED_RESPONSE_RETRIES = 3;

  private static final String SYSTEM_MESSAGE = "You are a bioinformatician working for VEuPathDB.org. You are an expert at providing biologist-friendly summaries of transcriptomic data";

  // Prepare JSON schemas for structured responses
  private static final JsonSchema.Schema experimentResponseSchema = JsonSchema.Schema.builder()
    .putAdditionalProperty("type", JsonValue.from("object"))
    .putAdditionalProperty("properties", JsonValue.from(Map.of(
          "one_sentence_summary", Map.of("type", "string"),
          "biological_importance", Map.of("type", "integer", "minimum", 0, "maximum", 5),
          "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 5),
          "experiment_keywords", Map.of("type", "array", "items", Map.of("type", "string")),
          "notes", Map.of("type", "string")
    )))
    .putAdditionalProperty("required", JsonValue.from(List.of(
          "one_sentence_summary",
          "biological_importance",
          "confidence",
          "experiment_keywords",
          "notes"
    )))
    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
    .build();

  private static final JsonSchema.Schema finalResponseSchema = JsonSchema.Schema.builder()
    .putAdditionalProperty("type", JsonValue.from("object"))
    .putAdditionalProperty("properties", JsonValue.from(Map.of(
          "headline", Map.of("type", "string"),
          "one_paragraph_summary", Map.of("type", "string"),
          "topics", Map.of("type", "array", "minimum", 1, "items", Map.of(
              "type", "object",
              "required", List.of("headline", "one_sentence_summary", "dataset_ids"),
              "properties", Map.of(
                  "headline", Map.of("type", "string"),
                  "one_sentence_summary", Map.of("type", "string"),
                  "dataset_ids", Map.of("type", "array", "items", Map.of("type", "string"))
              ),
              "additionalProperties", JsonValue.from(false)
          ))
    )))
    .putAdditionalProperty("required", JsonValue.from(List.of(
          "headline",
          "one_paragraph_summary",
          "topics"
    )))
    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
    .build();

  private static final String OPENAI_API_KEY_PROP_NAME = "OPENAI_API_KEY";

  private final OpenAIClientAsync _openAIClient;
  private final DailyCostMonitor _costMonitor;

  private static final Logger LOG = Logger.getLogger(Summarizer.class);

  public Summarizer(WdkModel wdkModel, DailyCostMonitor costMonitor) throws WdkModelException {

    String apiKey = wdkModel.getProperties().get(OPENAI_API_KEY_PROP_NAME);
    if (apiKey == null) {
      throw new WdkModelException("WDK property '" + OPENAI_API_KEY_PROP_NAME + "' has not been set.");
    }

    _openAIClient = OpenAIOkHttpClientAsync.builder()
        .apiKey(apiKey)
        .maxRetries(32)  // Handle 429 errors
        .build();

    _costMonitor = costMonitor;
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
        "**Task**: In one sentence, summarize how this gene is expressed in the given experiment. Do not describe the experiment itself—focus on whether the gene is, or is not, substantially and/or significantly upregulated or downregulated with respect to the experimental conditions tested. Take extreme care to assert the correct directionality of the response, especially in experiments with only one or two samples. Additionally, estimate the biological importance of this profile relative to other experiments on an integer scale of 0 (lowest, no differential expression) to 5 (highest, marked differential expression), even though specific comparative data has not been included. Also estimate your confidence (also 0 to 5) in making the estimate and add optional notes if there are peculiarities or caveats that may aid interpretation and further analysis. Finally, provide some general experiment-based keywords that provide a bit more context to the gene-based expression summary.\n" +
        "**Purpose**: The one-sentence summary will be displayed to users in tabular form on our gene-page. Please wrap user-facing species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers. The notes, scores and keywords will not be shown to users, but will be passed along with the summary to a second AI summarisation step that synthesizes insights from multiple experiments.\n" +
        "**Further guidance**: The `y_axis` field describes the `value` field in the `data` array, which is the primary expression level datum. Note that standard error statistics are only available when biological replicates were performed. However, percentile-normalized values can also guide your assessment of importance. If this is a time-series experiment, consider if it is cyclical and assess periodicity as appropriate. Ignore all discussion of individual or groups of genes in the experiment `description`, as this is irrelevant to the gene you are summarising. For RNA-Seq experiments, be aware that if `paralog_number` is high, interpretation may be tricky (consider both unique and non-unique counts if available). Ensure that each key appears exactly once in the JSON response. Do not include any duplicate fields.";
  }

  public CompletableFuture<JSONObject> describeExperiment(ExperimentInputs experimentInputs) {

    ChatCompletionCreateParams request = buildAiRequest(
        "experiment-summary",
        experimentResponseSchema,
        getExperimentMessage(experimentInputs.getExperimentData()));

    return getValidatedAiResponse("dataset " + experimentInputs.getDatasetId(), request, json -> {
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
        "Generate a one-paragraph summary (~100 words) describing the gene's expression. Structure it using <strong>, <ul>, and <li> tags with no attributes. If relevant, briefly speculate on the gene's potential function, but only if justified by the data. Also, generate a short, specific headline for the summary. The headline must reflect this gene's expression and **must not** include generic phrases like \"comprehensive insights into\" or the word \"gene\".\n\n" +
    "Additionally, group the per-experiment summaries (identified by `dataset_id`) with `biological_importance > 3` and `confidence > 3` into sections by topic. For each topic, provide:\n" +
    "- A headline summarizing the key experimental results within the topic\n" +
    "- A concise one-sentence summary of the topic's experimental results\n\n" +
    "These topics will be displayed to users. In all generated text, wrap species names in `<i>` tags and use clear, precise scientific language accessible to non-native English speakers.";
  }
  
  public JSONObject summarizeExperiments(String geneId, List<JSONObject> experiments) {

    ChatCompletionCreateParams request = buildAiRequest(
        "expression-summary",
        finalResponseSchema,
        getFinalSummaryMessage(experiments));

    return getValidatedAiResponse("summary for gene " + geneId, request, json ->
      // quality control (remove bad `dataset_id`s) and add 'Others' section for any experiments not listed by AI
      consolidateSummary(json, experiments)
    ).join();
  }

  private static JSONObject consolidateSummary(JSONObject summaryResponse,
      List<JSONObject> individualResults) {
    // Gather all dataset IDs from individualResults and map them to summaries.
    // Preserving the order of individualResults.
    Map<String, JSONObject> datasetSummaries = new LinkedHashMap<>();
    for (JSONObject result : individualResults) {
      datasetSummaries.put(result.getString("dataset_id"), result);
    }

    Set<String> seenDatasetIds = new LinkedHashSet<>();
    JSONArray deduplicatedTopics = new JSONArray();
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
        deduplicatedTopics.put(topic);
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
      deduplicatedTopics.put(otherTopic);
    }

    // Create final deduplicated summary
    JSONObject finalSummary = new JSONObject(summaryResponse.toString());
    finalSummary.put("topics", deduplicatedTopics);
    return finalSummary;
  }


  private static ChatCompletionCreateParams buildAiRequest(String name, Schema schema, String userMessage) {
    return ChatCompletionCreateParams.builder()
        .model(OPENAI_CHAT_MODEL)
        .maxCompletionTokens(MAX_RESPONSE_TOKENS)
        .responseFormat(ResponseFormatJsonSchema.builder()
            .jsonSchema(JsonSchema.builder()
                .name(name)
                .schema(schema)
                .strict(true)
                .build())
            .build())
        .addSystemMessage(SYSTEM_MESSAGE)
        .addUserMessage(userMessage)
        .build();
  }

  private CompletableFuture<JSONObject> getValidatedAiResponse(
      String operationDescription,
      ChatCompletionCreateParams request,
      Function<JSONObject,JSONObject> createFinalJson
  ) {
    return _openAIClient.chat().completions().create(request).thenApply(completion -> {

      // update cost accumulator
      _costMonitor.updateCost(completion.usage());

      // expect response to be a JSON string
      String jsonString = completion.choices().get(0).message().content().get();
      int attempts = 1;
      Exception mostRecentError;

      do {
        try {
          // convert to JSON object
          JSONObject jsonObject = new JSONObject(jsonString);

          // convert AI response JSON into final JSON we want to store
          return createFinalJson.apply(jsonObject);
        }
        catch (JSONException e) {
          mostRecentError = e;
          LOG.warn("Malformed JSON from OpenAI (attempt " + attempts + ") for " + operationDescription + ". Retrying...");

          // Re-request from OpenAI
          completion = _openAIClient.chat().completions().create(request).join();
          _costMonitor.updateCost(completion.usage());
          jsonString = completion.choices().get(0).message().content().get();
          attempts++;
        }
      }
      while (attempts <= MAX_MALFORMED_RESPONSE_RETRIES);

      // attempts have expired
      String message = "Failed to parse JSON after " + MAX_MALFORMED_RESPONSE_RETRIES + " attempts for " +
          operationDescription + ". Raw response: " + jsonString;
      LOG.error(message, mostRecentError);
      throw new RuntimeException(message, mostRecentError);
    });
  }
}
