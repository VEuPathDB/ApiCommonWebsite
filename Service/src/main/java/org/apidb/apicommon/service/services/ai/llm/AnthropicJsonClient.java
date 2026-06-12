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
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin wrapper around the Anthropic API for the JSON-producing prompt stage
 * ({@code getGeneSummary}). Responsibilities:
 * load the stage's prompt resource files, substitute {@code [PLACEHOLDER]}
 * markers, call the API (prefilling the assistant turn with {@code "{"}),
 * strip any markdown fences, and parse the JSON — retrying via a formatter LLM
 * up to {@code MAX_RETRY=3} times on malformed output (port of Python
 * {@code extract_json} + the STEP_1 formatter-retry loop).
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
   * here is fine). The assistant {@code prefill} is prepended to the model's
   * completion, matching the Python {@code call_prompt}.
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

    return (system, userPrompts, prefill) -> {
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
      if (prefill != null && !prefill.isEmpty()) {
        request.addAssistantMessage(prefill);
      }

      Message response = client.messages().create(request.build()).join();
      String text = response.content().stream()
          .flatMap(block -> block.text().stream())
          .map(textBlock -> textBlock.text())
          .findFirst()
          .orElse("");
      // The prefill is not echoed back by the API, so prepend it (Python parity).
      return prefill == null ? text : prefill + text;
    };
  }

  /**
   * Render the named prompt stage with the given placeholder replacements,
   * call the API, and return the parsed JSON response.
   *
   * @param stage        prompt-stage directory name, e.g. {@code getGeneSummary}
   * @param replacements placeholder → value, e.g. {@code [GENE] → "PF3D7_1133400"}
   */
  public JsonNode complete(String stage, Map<String, String> replacements) throws WdkModelException {
    Map<String, String> repl = new HashMap<>(replacements);
    repl.put("JSON_SCHEMA", _loader.schema(stage));

    String system = PromptLoader.render(_loader.system(stage), repl);
    List<String> userTurns = new ArrayList<>();
    for (String turn : _loader.userTurns(stage)) {
      userTurns.add(PromptLoader.render(turn, repl));
    }

    String raw = _completer.complete(system, userTurns, PREFILL);
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
            Collections.singletonList(formatterUserPrompt(current, errorMessage)), PREFILL);
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

  /**
   * Assistant prefill, forcing the model to start a JSON object (Python {@code prefill_text}).
   * Prefill is not supported on Sonnet 4.6+ / Opus 4.6+ / Fable 5 — see
   * {@link AiSummaryConfig#MODEL_NAME} before upgrading the model.
   */
  private static final String PREFILL = "{";

  /** Formatter-LLM system prompt, verbatim from the Python STEP_1 retry loop. */
  private static final String FORMATTER_SYSTEM_PROMPT =
      "Convert the supplied string to parsable JSON. respong with the corrected JSON ONLY and nothing else.";
}
