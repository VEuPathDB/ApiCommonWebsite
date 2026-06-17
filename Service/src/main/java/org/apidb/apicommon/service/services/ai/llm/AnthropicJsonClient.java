package org.apidb.apicommon.service.services.ai.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apidb.apicommon.service.services.ai.AiSummaryConfig;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin wrapper around the Anthropic API for the JSON-producing prompt stage
 * ({@code getGeneSummary}). Responsibilities:
 * load the stage's prompt resource files, substitute {@code [PLACEHOLDER]}
 * markers, call the API (constraining output to the stage's JSON schema via
 * structured outputs), strip any markdown fences, and parse the JSON — retrying
 * via a formatter LLM up to {@code MAX_RETRY=3} times on malformed output (port
 * of Python {@code extract_json} + the STEP_1 formatter-retry loop).
 *
 * <p>Client setup mirrors {@code ClaudeSummarizer}
 * ({@code AnthropicOkHttpClientAsync.builder().apiKey(...)}). The shared
 * {@code Summarizer} and its disk cache are intentionally NOT reused.
 */
public class AnthropicJsonClient implements JsonPromptClient {

  public static final String CLAUDE_API_KEY_PROP_NAME = "CLAUDE_API_KEY";

  /** Formatter-LLM retry attempts on malformed JSON (Python {@code max_retry}). */
  public static final int MAX_RETRY = 3;

  private final LlmCompleter _completer;
  private final PromptLoader _loader;

  public AnthropicJsonClient(WdkModel wdkModel) throws WdkModelException {
    this(buildCompleter(wdkModel), new PromptLoader());
  }

  /** Package-private seam: inject a completer + loader to avoid network in tests. */
  AnthropicJsonClient(LlmCompleter completer, PromptLoader loader) {
    _completer = completer;
    _loader = loader;
  }

  /** Max output tokens for the summary stages (Python {@code max_tokens}). */
  private static final long MAX_TOKENS = 20000L;

  /** Deterministic sampling, matching the Python pipeline ({@code model_temp = 0}). */
  private static final double TEMPERATURE = 0.0;

  /**
   * Build the production completer: an {@link AnthropicClientAsync} configured
   * like {@code ClaudeSummarizer}, wrapped to issue one blocking message call
   * per request (the pipeline runs on its own bounded-pool thread, so blocking
   * here is fine). JSON output is constrained via structured outputs
   * ({@code output_config.format}) rather than an assistant prefill, which is
   * unsupported on Sonnet 4.6+ (see {@link AiSummaryConfig#MODEL_NAME}).
   */
  private static LlmCompleter buildCompleter(WdkModel wdkModel) throws WdkModelException {
    String apiKey = wdkModel.getProperties().get(CLAUDE_API_KEY_PROP_NAME);
    if (apiKey == null) {
      throw new WdkModelException("WDK property '" + CLAUDE_API_KEY_PROP_NAME + "' has not been set.");
    }
    AnthropicClientAsync client = AnthropicOkHttpClientAsync.builder()
        .apiKey(apiKey)
        .maxRetries(32)  // handle 429s, mirroring ClaudeSummarizer
        .checkJacksonVersionCompatibility(false)
        .build();

    return (system, userPrompts, jsonSchema) -> {
      MessageCreateParams.Builder request = MessageCreateParams.builder()
          .model(AiSummaryConfig.MODEL_NAME)
          .maxTokens(MAX_TOKENS)
          .temperature(TEMPERATURE);
      if (system != null && !system.isEmpty()) {
        request.system(system);
      }
      for (String turn : userPrompts) {
        request.addUserMessage(turn);
      }
      if (jsonSchema != null && !jsonSchema.isEmpty()) {
        request.outputConfig(OutputConfig.builder()
            .format(JsonOutputFormat.builder()
                .schema(buildSchema(jsonSchema))
                .build())
            .build());
      }

      Message response = client.messages().create(request.build()).join();
      return response.content().stream()
          .flatMap(block -> block.text().stream())
          .map(textBlock -> textBlock.text())
          .findFirst()
          .orElse("");
    };
  }

  /**
   * Parse a JSON-schema string (from disk) into the SDK's freeform
   * {@link JsonOutputFormat.Schema} by lifting each top-level key onto the
   * schema object.
   */
  private static JsonOutputFormat.Schema buildSchema(String jsonSchema) throws WdkModelException {
    try {
      Map<String, Object> schemaMap = JSON.readValue(jsonSchema, new TypeReference<Map<String, Object>>() {});
      JsonOutputFormat.Schema.Builder builder = JsonOutputFormat.Schema.builder();
      for (Map.Entry<String, Object> entry : schemaMap.entrySet()) {
        builder.putAdditionalProperty(entry.getKey(), JsonValue.from(entry.getValue()));
      }
      return builder.build();
    }
    catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new WdkModelException("Invalid JSON schema for structured output", e);
    }
  }

  /**
   * Render the named prompt stage with the given placeholder replacements,
   * call the API, and return the parsed JSON response.
   *
   * @param stage        prompt-stage directory name, e.g. {@code getGeneSummary}
   * @param replacements placeholder → value, e.g. {@code [GENE] → "PF3D7_1133400"}
   */
  public JsonNode complete(String stage, Map<String, String> replacements) throws WdkModelException {
    // The schema is sent to the model via structured outputs (output_config.format),
    // so it is no longer embedded in the prompt text.
    String schema = _loader.schema(stage);
    Map<String, String> repl = new HashMap<>(replacements);

    String system = PromptLoader.render(_loader.system(stage), repl);
    List<String> userTurns = new ArrayList<>();
    for (String turn : _loader.userTurns(stage)) {
      userTurns.add(PromptLoader.render(turn, repl));
    }

    String raw = _completer.complete(system, userTurns, schema);
    JsonNode parsed = extractJson(raw);

    // Formatter-retry loop (port of STEP_1_single_pair_processing): while the
    // text is not yet valid JSON, ask a formatter LLM to repair it, up to
    // MAX_RETRY times, feeding back the last raw text and the parse error.
    String current = raw;
    String errorMessage = "Initial attempt failed to produce valid JSON.";
    int attempt = 1;
    while (parsed == null && attempt <= MAX_RETRY) {
      try {
        String formatted = _completer.complete(FORMATTER_SYSTEM_PROMPT,
            Collections.singletonList(formatterUserPrompt(current, errorMessage)), schema);
        current = formatted;
        parsed = extractJson(formatted);
      }
      catch (Exception e) {
        errorMessage = "parsing JSON failed: " + e.getMessage();
      }
      attempt++;
    }

    if (parsed == null) {
      throw new WdkModelException(
          "Max retries exceeded: could not parse valid JSON from the LLM for stage '" + stage + "'");
    }
    return parsed;
  }

  private static String formatterUserPrompt(String failedText, String errorMessage) {
    return "I tried parsing this JSON: <JSON> " + failedText + " </JSON> \n "
        + "The following error message popped up: " + errorMessage;
  }

  /**
   * Strip markdown {@code ```json}/{@code ```} fences and parse to JSON. Returns
   * {@code null} on a parse failure (Python {@code extract_json} returns the raw
   * text in that case; the retry loop treats either as "not yet valid JSON").
   */
  public static JsonNode extractJson(String text) {
    if (text == null) return null;
    String stripped = text.strip().replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "");
    try {
      return JSON.readTree(stripped);
    }
    catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return null;
    }
  }

  private static final ObjectMapper JSON = new ObjectMapper();

  /** Formatter-LLM system prompt, verbatim from the Python STEP_1 retry loop. */
  private static final String FORMATTER_SYSTEM_PROMPT =
      "Convert the supplied string to parsable JSON. respong with the corrected JSON ONLY and nothing else.";
}
