package org.apidb.apicommon.model.report.ai.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apidb.apicommon.model.report.ai.expression.GeneRecordProcessor.GeneSummaryInputs;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.JsonValue;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema;

public class Summarizer {

  private static final Logger LOG = Logger.getLogger(Summarizer.class);

  // provide exact model number for semi-reproducibility
  public static final ChatModel OPENAI_CHAT_MODEL = ChatModel.GPT_4O_2024_11_20; // GPT_4O_2024_08_06;

  private static final int MAX_RESPONSE_TOKENS = 10000;
    
  private static final String systemMessage = "You are a bioinformatician working for VEuPathDB.org. You are an expert at providing biologist-friendly summaries of transcriptomic data";

  // Prepare JSON schemas for structured responses
  // NOTE: this code is horrible to look at/read. It would be better to just define the schemas as JSON strings
  // but this is only really nice when we have """ text block """ support, coming soon when we upgrade, perhaps?
  private static final JsonSchema.Schema experimentResponseSchema =
    JsonSchema.Schema.builder()
    .putAdditionalProperty("type", JsonValue.from("object"))
    .putAdditionalProperty("properties",
                           JsonValue
                           .from(Map
                                 .of(
                                     "one_sentence_summary", Map.of("type", "string"),
                                     "biological_importance", Map.of("type", "integer", "minimum", 0, "maximum", 5),
                                     "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 5),
                                     "experiment_keywords", Map.of("type", "array", "items", Map.of("type", "string")), 
                                     "notes", Map.of("type", "string")
                                     )
                                 )
                           )
    .putAdditionalProperty("required",
                           JsonValue.from(
                                          List.of(
                                                  "one_sentence_summary",
                                                  "biological_importance",
                                                  "confidence",
                                                  "experiment_keywords",
                                                  "notes")
                                          )
                           )
    .build();

  private static final JsonSchema.Schema finalResponseSchema =
    JsonSchema.Schema.builder()
    .putAdditionalProperty("type", JsonValue.from("object"))
    .putAdditionalProperty("properties",
                           JsonValue
                           .from(Map
                                 .of(
                                     "headline", Map.of("type", "string"),
                                     "one_paragraph_summary", Map.of("type", "string"),
                                     "sections",
                                     Map.of("type", "array",
                                            "minimum", 1,
                                            "items",
                                            Map.of(
                                                   "type", "object",
                                                   "required", List.of("headline", "one_sentence_summary", "dataset_ids"),
                                                   "properties",
                                                   Map.of(
                                                          "headline", Map.of("type", "string"),
                                                          "one_sentence_summary", Map.of("type", "string"),
                                                          "dataset_ids", Map.of("type", "array",
                                                                                "items", Map.of("type", "string"))
                                                          )
                                                   )
                                            )
                                     )
                                 )
                           )
    .putAdditionalProperty("required",
                           JsonValue.from(
                                          List.of(
                                                  "headline",
                                                  "one_paragraph_summary",
                                                  "dataset_ids"
                                                  )
                                          )
                           )
    .build();

  private static final String OPENAI_API_KEY_PROP_NAME = "OPENAI_API_KEY";

  private final OpenAIClientAsync _openAIClient;

  public Summarizer(WdkModel wdkModel) {
    _openAIClient = OpenAIOkHttpClientAsync.builder()
        .apiKey(wdkModel.getProperties().get(OPENAI_API_KEY_PROP_NAME))
        .maxRetries(32)  // Handle 429 errors
        .build();
  }

  public JSONObject summarizeExpression(GeneSummaryInputs summaryInputs, boolean populateIfNotPresent)
      throws WdkUserException {

    try {

      // Process expression data further into a list of pruned metadata plus data
      List<JSONObject> experimentsWithData = summaryInputs.getExperimentsWithData();
      String geneId = summaryInputs.getGeneId();
      System.out.println("Pre-processed Experiments: " + experimentsWithData.size());

      // TEST Mode: Collect valid cache entries
      if (!populateIfNotPresent) {
        List<JSONObject> cachedResponses = new ArrayList<>();

        for (JSONObject experiment : experimentsWithData) {

          Optional<JSONObject> experimentSummary = sendExperimentToOpenAI(geneId, experiment, populateIfNotPresent).join();

          if (experimentSummary.isPresent()) {
            cachedResponses.add(experimentSummary.get());
          }
          else {
            return new JSONObject().put("cacheStatus", "miss"); // If any cache entry is missing, return early
          }
        }

        // All experiment-level caches are valid, now check final summary cache
        Optional<JSONObject> finalSummary = sendExperimentSummariesToOpenAI(geneId, cachedResponses, populateIfNotPresent);

        return finalSummary.map(summary -> new JSONObject().put("cacheStatus", "hit").put("expressionSummary",
            summary)).orElseGet(() -> new JSONObject().put("cacheStatus", "miss"));
      }

      // Send AI requests in parallel
      List<CompletableFuture<Optional<JSONObject>>> aiRequests = experimentsWithData.stream()
          // TO DO - potentially some optimisation?
          // .map(exp -> CompletableFuture.supplyAsync(() -> sendExperimentToOpenAI(geneId, exp,
          // CacheMode.POPULATE)))
          .map(exp -> sendExperimentToOpenAI(geneId, exp, populateIfNotPresent)).collect(Collectors.toList());
      // Wait for all requests to complete with `join`
      List<JSONObject> responses = aiRequests.stream().map(CompletableFuture::join) // Get
                                                                                    // Optional<JSONObject>
          .filter(Optional::isPresent) // Keep only non-empty results
          .map(Optional::get) // Extract JSONObject
          .collect(Collectors.toList());

      // Debug output
      // System.out.println("Individual responses:");
      // responses.forEach(response -> System.out.println(response.toString(2)));
      // System.exit(0);

      Optional<JSONObject> finalSummary = sendExperimentSummariesToOpenAI(geneId, responses, populateIfNotPresent);
      return finalSummary.map(
          summary -> new JSONObject().put("cacheStatus", "hit").put("expressionSummary", summary)).orElseGet(
              () -> new JSONObject().put("cacheStatus", "miss"));
    }
    catch (WdkModelException e) {

      // Handle errors gracefully
      System.err.println("Error fetching expression data: " + e.getMessage());
      throw new WdkUserException(e);
    }
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
    JSONObject experimentForAI = new JSONObject(experiment.toString()); // clone
    experimentForAI.remove("dataset_id");

    return
        "The JSON below contains expression data for a single gene within a specific experiment, along with relevant experimental and bioinformatics metadata:\n\n" +
        String.format("```json\n%s\n```\n\n", JsonUtil.serialize(experimentForAI)) +
        "**Task**: In one sentence, summarize how this gene is expressed in the given experiment. Do not describe the experiment itself—focus on whether the gene is, or is not, substantially and/or significantly upregulated or downregulated with respect to the experimental conditions tested. Take extreme care to assert the correct directionality of the response, especially in experiments with only one or two samples. Additionally, estimate the biological importance of this profile relative to other experiments on an integer scale of 0 (lowest, no differential expression) to 5 (highest, marked differential expression), even though specific comparative data has not been included. Also estimate your confidence (also 0 to 5) in making the estimate and add optional notes if there are peculiarities or caveats that may aid interpretation and further analysis. Finally, provide some general experiment-based keywords that provide a bit more context to the gene-based expression summary.\n" +
        "**Purpose**: The one-sentence summary will be displayed to users in tabular form on our gene-page. Please wrap user-facing species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers. The notes, scores and keywords will not be shown to users, but will be passed along with the summary to a second AI summarisation step that synthesizes insights from multiple experiments.\n" +
        "**Further guidance**: The `y_axis` field describes the `value` field in the `data` array, which is the primary expression level datum. Note that standard error statistics are only available when biological replicates were performed. However, percentile-normalized values can also guide your assessment of importance. If this is a time-series experiment, consider if it is cyclical and assess periodicity as appropriate. Ignore all discussion of individual or groups of genes in the experiment `description`, as this is irrelevant to the gene you are summarising. For RNA-Seq experiments, be aware that if `paralog_number` is high, interpretation may be tricky (consider both unique and non-unique counts if available). Ensure that each key appears exactly once in the JSON response. Do not include any duplicate fields.";
  }

  private CompletableFuture<Optional<JSONObject>> sendExperimentToOpenAI(String geneId,
      JSONObject experiment, boolean populateIfNotPresent) {

    String cacheKey = geneId + ':' + datasetId;

    if (_cache.isCacheValid(cacheKey, message)) {
      try {
        JSONObject cachedResponse = cache.readCachedData(cacheKey);
        return CompletableFuture.completedFuture(Optional.of(cachedResponse));
      }
      catch (Exception e) {
        System.err.println("Cache read failed for key " + cacheKey + ": " + e.getMessage());

        if (!populateIfNotPresent) {
          return CompletableFuture.completedFuture(Optional.empty()); // Treat as cache miss
        }
        // Else, log and fall through to AI generation
      }
    }
    else if (!populateIfNotPresent) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder().model(
        OPENAI_CHAT_MODEL).maxCompletionTokens(MAX_RESPONSE_TOKENS).responseFormat(
            ResponseFormatJsonSchema.builder().jsonSchema(
                JsonSchema.builder().name("experiment-summary").schema(
                    experimentResponseSchema).build()).build()).addSystemMessage(
                        systemMessage).addUserMessage(message).build();

    // add dataset_id back to the response
    return _openAIClient.chat().completions().create(request).thenApply(completion -> {
      // response is a JSON string
      String jsonString = completion.choices().get(0).message().content().get();
      try {
        JSONObject jsonObject = new JSONObject(jsonString);
        jsonObject.put("dataset_id", datasetId);

        // Cache the response
        try {
          cache.populateCache(cacheKey, message, jsonObject);
        }
        catch (Exception e) {
          System.err.println("Warning: Failed to cache response for gene " + geneId + " and dataset " +
              datasetId + ": " + e.getMessage());
        }

        return Optional.of(jsonObject);
      }
      catch (JSONException e) {
        System.err.println("Error parsing JSON response for gene " + geneId + " and dataset " + datasetId +
            ": " + e.getMessage());
        System.err.println("Raw response: " + jsonString);
        JSONObject errorResponse = new JSONObject().put("error", "Invalid JSON response").put("dataset_id",
            datasetId);
        return Optional.of(errorResponse);
      }
    });
  }

  private Optional<JSONObject> sendExperimentSummariesToOpenAI(String geneId,
      List<JSONObject> experiments, boolean populateIfNotPresent) {

    String message = String.format(
        "Below are AI-generated summaries of a gene's behaviour in multiple transcriptomics experiments, provided in JSON format:\n\n" +
            "```json\n%s\n```\n\n",
        new JSONArray(experiments).toString()) +
        "Provide a snappy headline and a one-paragraph summary of the gene's expression characteristics that gives the most biological insight into its function. Both are for human consumption on the gene page of our website. Also organise the experimental results (identified by `dataset_id`) into sections, ordered by descending biological importance. Provide a headline and one-sentence summary for each section. These will also be shown to users. Wrap species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers throughout your response.";

    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder().model(
        OPENAI_CHAT_MODEL).maxCompletionTokens(MAX_RESPONSE_TOKENS).responseFormat(
            ResponseFormatJsonSchema.builder().jsonSchema(
                JsonSchema.builder().name("expression-summary").schema(
                    finalResponseSchema).build()).build()).addSystemMessage(systemMessage).addUserMessage(
                        message).build();

    // System.out.println(message);

    ChatCompletion completion = _openAIClient.chat().completions().create(request).join(); // join() waits for
                                                                                          // the async
                                                                                          // response
    String jsonString = completion.choices().get(0).message().content().get();
    JSONObject rawResponseObject = new JSONObject(jsonString);

    // quality control (remove bad `dataset_id`s) and add 'Others' section for any experiments not listed by
    // AI
    JSONObject finalResponseObject = consolidateSummary(rawResponseObject, experiments);

    return Optional.of(finalResponseObject);
  }

  public static JSONObject consolidateSummary(JSONObject summaryResponse,
      List<JSONObject> individualResults) {
    // Gather all dataset IDs from individualResults and map them to summaries
    Map<String, JSONObject> datasetSummaries = new HashMap<>();
    for (JSONObject result : individualResults) {
      datasetSummaries.put(result.getString("dataset_id"), result);
    }

    Set<String> seenDatasetIds = new HashSet<>();
    JSONArray deduplicatedSections = new JSONArray();
    JSONArray sections = summaryResponse.getJSONArray("sections");

    for (int i = 0; i < sections.length(); i++) {
      JSONObject section = sections.getJSONObject(i);
      JSONArray datasetIds = section.getJSONArray("dataset_ids");
      JSONArray summaries = new JSONArray();

      for (int j = 0; j < datasetIds.length(); j++) {
        String id = datasetIds.getString(j);

        // Warn and skip if the id doesn't exist
        if (!datasetSummaries.containsKey(id)) {
          System.out.println(
              "WARNING: summary section id '" + id + "' does not exist. Excluding from final output.");
          continue;
        }
        // Skip if we've seen it
        if (seenDatasetIds.contains(id))
          continue;

        seenDatasetIds.add(id);
        summaries.put(datasetSummaries.get(id));
      }

      // Update section with mapped summaries and remove dataset_ids key
      section.put("summaries", summaries);
      section.remove("dataset_ids");
      deduplicatedSections.put(section);
    }

    // Find missing dataset IDs
    Set<String> missingDatasetIds = new HashSet<>(datasetSummaries.keySet());
    missingDatasetIds.removeAll(seenDatasetIds);

    // If there are missing IDs, add an "Others" section
    if (!missingDatasetIds.isEmpty()) {
      JSONArray otherSummaries = new JSONArray();
      for (String id : missingDatasetIds) {
        otherSummaries.put(datasetSummaries.get(id));
      }

      JSONObject otherSection = new JSONObject();
      otherSection.put("headline", "Other");
      otherSection.put("one_sentence_summary",
          "These experiments were not grouped into sub-sections by the AI.");
      otherSection.put("summaries", otherSummaries);
      deduplicatedSections.put(otherSection);
    }

    // Create final deduplicated summary
    JSONObject finalSummary = new JSONObject(summaryResponse.toString());
    finalSummary.put("sections", deduplicatedSections);
    return finalSummary;
  }

}

