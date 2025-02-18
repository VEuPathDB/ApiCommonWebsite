package org.apidb.apicommon.model.report.ai.expression;

import org.apidb.apicommon.model.report.ai.CacheMode;
import org.apidb.apicommon.model.report.ai.expression.AiExpressionCache;

import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.WdkModelException;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.ChatCompletion;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema;
import com.openai.core.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.io.IOException;

public class Summarizer {
  private static final OpenAIClientAsync openAIClient = OpenAIOkHttpClientAsync.builder()
    .fromEnv()  // Uses OPENAI_API_KEY from env
    .maxRetries(32)  // Handle 429 errors
    .build();

  private static final AiExpressionCache cache;

  static {
    AiExpressionCache tempCache = null;
    try {
      tempCache = new AiExpressionCache();
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize AiExpressionCache", e);
    }
    cache = tempCache;
  }
  
  // provide exact model number for semi-reproducibility
  private static final ChatModel model = ChatModel.GPT_4O_2024_11_20; // GPT_4O_2024_08_06;
  private static int MAX_RESPONSE_TOKENS = 10000;
    
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
   
  public static JSONObject summarizeExpression(RecordInstance geneRecord, CacheMode cacheMode) throws WdkUserException  {
        
    try {
      // Process expression data further into a list of pruned metadata plus data
      List<JSONObject> experimentsWithData = GeneRecordProcessor.processExpressionData(geneRecord);
      System.out.println("Pre-processed Experiments: " + experimentsWithData.size());
	    
      // Send AI requests in parallel
      // CACHE OPPORTUNITY ONE - sendExperimentToOpenAI
      List<CompletableFuture<JSONObject>> aiRequests = experimentsWithData.stream()
        .map(Summarizer::sendExperimentToOpenAI)
        .collect(Collectors.toList());
      // Wait for all requests to complete
      List<JSONObject> responses = aiRequests.stream()
        .map(CompletableFuture::join)  // Blocks until each completes
        .collect(Collectors.toList());

      // Debug output
      // System.out.println("Individual responses:");
      // responses.forEach(response -> System.out.println(response.toString(2)));
      // System.exit(0);
	    
      JSONObject finalSummary = sendExperimentSummariesToOpenAI(responses);
      return finalSummary;

    } catch (WdkModelException e) {
      // Handle errors gracefully
      System.err.println("Error fetching expression data: " + e.getMessage());
      throw new WdkUserException(e);
    }
  }

  
  private static CompletableFuture<JSONObject> sendExperimentToOpenAI(JSONObject experiment) {
    return sendExperimentToOpenAI(experiment, CacheMode.POPULATE);
  }
  
  private static CompletableFuture<JSONObject> sendExperimentToOpenAI(JSONObject experiment, CacheMode cacheMode) {

    // Possible TO DO: AI EDIT DESCRIPTION
    // Before sending the experiment+data to the AI, ask the AI to edit the `description` field
    // as follows: (This should be cached by dataset_id only and would be called once per organism
    // and would reduce tokens and "cognitive load" a little bit for the next step.)
    //
    // "Edit the following text to so that it **only** describes the experimental design of the transcriptomics part of the study. Do not mention the results of any bioinformatics analyses performed, especially not any genes or groups of genes and their expression behaviour."
    //
    // We would then be able to remove the "Ignore all discussion of individual or groups of genes in the experiment `description`, as this is irrelevant to the gene you are summarising." from the prompt below.


	
    // We don't need to send the gene_id or dataset_id to the AI but we need the gene ID
    // for the cache key and it's useful to have dataset_id in the response for phase two
    // - so we save them for later
    JSONObject experimentForAI = new JSONObject(experiment.toString()); // clone
    String geneId = experimentForAI.has("gene_id") ? experimentForAI.getString("gene_id") : null;
    experimentForAI.remove("gene_id");
    String datasetId = experimentForAI.has("dataset_id") ? experimentForAI.getString("dataset_id") : null;
    experimentForAI.remove("dataset_id");
    
    String message = "The JSON below contains expression data for a single gene within a specific experiment, along with relevant experimental and bioinformatics metadata:\n\n" +
	    "```json\n%s\n```\n\n".formatted(experimentForAI.toString()) +
	    "**Task**: In one sentence, summarize how this gene is expressed in the given experiment. Do not describe the experiment itself—focus on whether the gene is, or is not, substantially and/or significantly upregulated or downregulated with respect to the experimental conditions tested. Take extreme care to assert the correct directionality of the response, especially in experiments with only one or two samples. Additionally, estimate the biological importance of this profile relative to other experiments on an integer scale of 0 (lowest, no differential expression) to 5 (highest, marked differential expression), even though specific comparative data has not been included. Also estimate your confidence (also 0 to 5) in making the estimate and add optional notes if there are peculiarities or caveats that may aid interpretation and further analysis. Finally, provide some general experiment-based keywords that provide a bit more context to the gene-based expression summary.\n" +
	    "**Purpose**: The one-sentence summary will be displayed to users in tabular form on our gene-page. Please wrap user-facing species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers. The notes, scores and keywords will not be shown to users, but will be passed along with the summary to a second AI summarisation step that synthesizes insights from multiple experiments.\n" +
	    "**Further guidance**: The `y_axis` field describes the `value` field in the `data` array, which is the primary expression level datum. Note that standard error statistics are only available when biological replicates were performed. However, percentile-normalized values can also guide your assessment of importance. If this is a time-series experiment, consider if it is cyclical and assess periodicity as appropriate. Ignore all discussion of individual or groups of genes in the experiment `description`, as this is irrelevant to the gene you are summarising. For RNA-Seq experiments, be aware that if `paralog_number` is high, interpretation may be tricky (consider both unique and non-unique counts if available). Ensure that each key appears exactly once in the JSON response. Do not include any duplicate fields.";

    //   System.out.println(message); /// DEBUG

    String cacheKey = geneId + ':' + datasetId;

    if (cache.isCacheValid(cacheKey, message)) {
      try {
	return CompletableFuture.completedFuture(cache.readCachedData(cacheKey));
//      } catch (IOException e) {
//        // maybe log that the cache was unexpectedly invalidated
//	// and then continue to compute and populate cache entry
      } catch (Exception e) {
	// do nothing
      }
    }
    
    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
      .model(model)
      .maxCompletionTokens(MAX_RESPONSE_TOKENS)
      .responseFormat(ResponseFormatJsonSchema.builder()
                      .jsonSchema(JsonSchema.builder()
                                  .name("experiment-summary")
                                  .schema(experimentResponseSchema)
                                  .build())
                      .build())
      .addSystemMessage(systemMessage)
      .addUserMessage(message)
	    // .temperature(1.0)
      .build();

    // add dataset_id back to the response
    return openAIClient.chat().completions().create(request)
	    .thenApply(completion -> {
          // response is a JSON string
          String jsonString = completion.choices().get(0).message().content().get();
          try {
            JSONObject jsonObject = new JSONObject(jsonString);
            jsonObject.put("dataset_id", datasetId);
            return jsonObject;
          } catch (JSONException e) {
            System.err.println("Error parsing JSON response for dataset " + datasetId + ": " + e.getMessage());
            System.err.println("Raw response: " + jsonString);
            return new JSONObject().put("error", "Invalid JSON response").put("dataset_id", datasetId);
          }
        });
  }


  private static JSONObject sendExperimentSummariesToOpenAI(List<JSONObject> experiments) {
	
    String message = "Below are AI-generated summaries of a gene's behaviour in multiple transcriptomics experiments, provided in JSON format:\n\n" +
	    "```json\n%s\n```\n\n".formatted(new JSONArray(experiments)) +
	    "Provide a snappy headline and a one-paragraph summary of the gene's expression characteristics that gives the most biological insight into its function. Both are for human consumption on the gene page of our website. Also organise the experimental results (identified by `dataset_id`) into sections, ordered by descending biological importance. Provide a headline and one-sentence summary for each section. These will also be shown to users. Wrap species names in `<i>` tags and use clear, scientific language accessible to non-native English speakers throughout your response.";

    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
      .model(model)
      .maxCompletionTokens(MAX_RESPONSE_TOKENS)
      .responseFormat(ResponseFormatJsonSchema.builder()
                      .jsonSchema(JsonSchema.builder()
                                  .name("expression-summary")
                                  .schema(finalResponseSchema)
                                  .build())
                      .build())
      .addSystemMessage(systemMessage)
      .addUserMessage(message)
      .build();

    // System.out.println(message);

    ChatCompletion completion = openAIClient.chat().completions().create(request).join(); // join() waits for the async response
    String jsonString = completion.choices().get(0).message().content().get();
    JSONObject rawResponseObject = new JSONObject(jsonString);

    // TO DO - quality control (remove bad `dataset_id`s) and add 'Others' section for any experiments not listed by AI
    JSONObject finalResponseObject = consolidateSummary(rawResponseObject, experiments);
	
    return finalResponseObject;
  }

    
  public static JSONObject consolidateSummary(JSONObject summaryResponse, List<JSONObject> individualResults) {
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
          System.out.println("WARNING: summary section id '" + id + "' does not exist. Excluding from final output.");
          continue;
        }
        // Skip if we've seen it
        if (seenDatasetIds.contains(id)) continue;
                
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
      otherSection.put("one_sentence_summary", "These experiments were not grouped into sub-sections by the AI.");
      otherSection.put("summaries", otherSummaries);
      deduplicatedSections.put(otherSection);
    }

    // Create final deduplicated summary
    JSONObject finalSummary = new JSONObject(summaryResponse.toString());
    finalSummary.put("sections", deduplicatedSections);
    return finalSummary;
  }

}

