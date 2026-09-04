package org.apidb.apicommon.model.report.ai.expression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Usage;

public class ClaudeSummarizer extends Summarizer {

  private static final Logger LOG = Logger.getLogger(ClaudeSummarizer.class);

  // Primary model. Plain id string, not a Model constant: anthropic-java 2.33.0
  // predates the claude-sonnet-5 alias, and MessageCreateParams.model(String) is
  // validated server-side. The alias (vs a dated snapshot like
  // claude-sonnet-4-5-20250929) tracks the current Sonnet 5 and does not retire.
  public static final String CLAUDE_MODEL = "claude-sonnet-5";

  // When CLAUDE_MODEL returns stop_reason=refusal (e.g. the API "bio" safety
  // classifier declining a transcriptomics prompt) the identical request is
  // re-sent once on this model. Anthropic's refusals-and-fallback guidance names
  // claude-opus-4-8, but we deliberately fall *down* to Haiku 4.5 instead: a
  // smaller model, less exposed to the newer research-biology classifier, and
  // cheaper for a retry path. Caveat: no long support runway (available to
  // ~mid-Oct 2026), though it should outlast the arrival of a Haiku 5.
  // Override with the optional WDK property CLAUDE_FALLBACK_MODEL; set it to
  // "none" to disable the fallback hop.
  public static final String DEFAULT_FALLBACK_CLAUDE_MODEL = "claude-haiku-4-5";

  public static final boolean USE_EXTENDED_THINKING = false;

  private static final String CLAUDE_API_KEY_PROP_NAME = "CLAUDE_API_KEY";
  private static final String CLAUDE_FALLBACK_MODEL_PROP_NAME = "CLAUDE_FALLBACK_MODEL";
  private static final String FALLBACK_DISABLED = "none";

  private final AnthropicClientAsync _claudeClient;
  private final String _fallbackModel;  // null when disabled

  public ClaudeSummarizer(WdkModel wdkModel, DailyCostMonitor costMonitor, boolean makeTopicEmbeddings) throws WdkModelException {
    super(wdkModel, costMonitor, makeTopicEmbeddings);

    String apiKey = wdkModel.getProperties().get(CLAUDE_API_KEY_PROP_NAME);
    if (apiKey == null) {
      throw new WdkModelException("WDK property '" + CLAUDE_API_KEY_PROP_NAME + "' has not been set.");
    }

    String configuredFallback = wdkModel.getProperties().get(CLAUDE_FALLBACK_MODEL_PROP_NAME);
    if (configuredFallback == null || configuredFallback.isBlank()) {
      _fallbackModel = DEFAULT_FALLBACK_CLAUDE_MODEL;
    }
    else if (FALLBACK_DISABLED.equalsIgnoreCase(configuredFallback.trim())) {
      _fallbackModel = null;
    }
    else {
      _fallbackModel = configuredFallback.trim();
    }

    _claudeClient = AnthropicOkHttpClientAsync.builder()
        .apiKey(apiKey)
        .maxRetries(32)  // Handle 429 errors
        .checkJacksonVersionCompatibility(false)
        .build();
  }

  @Override
  protected CompletableFuture<String> callApiForJson(String prompt, Map<String, Object> schema) {
    return sendMessage(CLAUDE_MODEL, prompt, schema)
        .thenCompose(response -> {
          // A safety-classifier refusal arrives as a normal HTTP 200 with
          // stop_reason=refusal and no content blocks. Re-send the identical
          // request once on the fallback model (unless it has been disabled
          // with CLAUDE_FALLBACK_MODEL=none).
          if (_fallbackModel == null || !isRefusal(response)) {
            return CompletableFuture.completedFuture(response);
          }
          logRefusal(response, "re-sending on fallback model " + _fallbackModel);
          return sendMessage(_fallbackModel, prompt, schema);
        })
        .thenApply(this::extractJsonText);
  }

  /**
   * One Messages API call for the given model id, wrapped in the shared
   * exponential-backoff retry for transient 5xx / overload errors.
   */
  private CompletableFuture<Message> sendMessage(String modelId, String prompt, Map<String, Object> schema) {
    MessageCreateParams.Builder requestBuilder = MessageCreateParams.builder()
        .model(modelId)
        .maxTokens(MAX_RESPONSE_TOKENS)
        .system(SYSTEM_MESSAGE)
        .outputConfig(OutputConfig.builder()
            .format(JsonOutputFormat.builder()
                .schema(toClaudeSchema(schema))
                .build())
            .build())
        .addUserMessage(prompt);

    if (USE_EXTENDED_THINKING) {
      requestBuilder.enabledThinking(1024);
    }

    MessageCreateParams request = requestBuilder.build();

    return retryOnOverload(
        () -> _claudeClient.messages().create(request),
        e -> e instanceof com.anthropic.errors.InternalServerException,
        "Claude API call (" + modelId + ")"
    );
  }

  // Keywords the platform-agnostic schemas use (for OpenAI's benefit) that Claude's
  // output_config.format rejects outright: "output_config.format.schema: For 'integer'
  // type, properties maximum, minimum are not supported" (400 invalid_request_error).
  // Per the JSON Schema Limitations in Anthropic's structured-outputs docs, numerical/
  // string/array bound constraints aren't supported at all - the Python/TS SDKs strip
  // these client-side automatically; the Java SDK does not, so we do it here.
  private static final Set<String> CLAUDE_UNSUPPORTED_SCHEMA_KEYWORDS = Set.of(
      "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
      "minLength", "maxLength", "pattern",
      "minItems", "maxItems", "uniqueItems"
  );

  private static JsonOutputFormat.Schema toClaudeSchema(Map<String, Object> schema) {
    Map<String, Object> sanitized = stripUnsupportedKeywords(schema);
    JsonOutputFormat.Schema.Builder builder = JsonOutputFormat.Schema.builder();
    // Looks like a shallow copy, but JsonValue.from() recursively serializes nested
    // Map/List values, so this is effectively a deep copy.
    sanitized.forEach((key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)));
    return builder.build();
  }

  private static Map<String, Object> stripUnsupportedKeywords(Map<String, Object> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!CLAUDE_UNSUPPORTED_SCHEMA_KEYWORDS.contains(entry.getKey())) {
        result.put(entry.getKey(), stripUnsupportedKeywords(entry.getValue()));
      }
    }
    return result;
  }
  
  private static Object stripUnsupportedKeywords(Object node) {
    if (node instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = (String) entry.getKey();
        if (!CLAUDE_UNSUPPORTED_SCHEMA_KEYWORDS.contains(key)) {
          result.put(key, stripUnsupportedKeywords(entry.getValue()));
        }
      }
      return result;
    }
    if (node instanceof List<?> list) {
      return list.stream().map(ClaudeSummarizer::stripUnsupportedKeywords).collect(Collectors.toList());
    }
    return node;
  }

  private static boolean isRefusal(Message response) {
    return response.stopReason()
        .map(stopReason -> StopReason.REFUSAL.asString().equals(stopReason.asString()))
        .orElse(false);
  }

  private static boolean isMaxTokens(Message response) {
    return response.stopReason()
        .map(stopReason -> StopReason.MAX_TOKENS.asString().equals(stopReason.asString()))
        .orElse(false);
  }

  // Truncates to the last `keepChars` characters, so a runaway/repetition-loop generation
  // that fills the entire max_tokens budget (observed: thousands of repeated characters)
  // doesn't blow up the log with tens of KB per line - only the tail matters for spotting
  // what it got stuck repeating.
  private static String lastChars(String text, int keepChars) {
    return text.length() <= keepChars ? text : "..." + text.substring(text.length() - keepChars);
  }

  private void logRefusal(Message response, String action) {
    LOG.warn("Claude model " + response.model() + " refused the request; " + action + ". id=" + response.id() +
        ", category=" + response.stopDetails().flatMap(details -> details.category()).map(Object::toString).orElse("<none>") +
        ", explanation=" + response.stopDetails().flatMap(details -> details.explanation()).orElse("<none>") +
        ", usage=" + response.usage());
  }

  private String extractJsonText(Message response) {
    // Convert Claude usage to TokenUsage for cost monitoring
    Usage claudeUsage = response.usage();
    TokenUsage tokenUsage = TokenUsage.builder()
        .promptTokens(claudeUsage.inputTokens())
        .completionTokens(claudeUsage.outputTokens())
        .build();

    _costMonitor.updateCost(tokenUsage);

    // Extract text from content blocks using stream API
    Optional<String> rawText = response.content().stream()
        .flatMap(contentBlock -> contentBlock.text().stream())
        .map(textBlock -> textBlock.text())
        .findFirst();

    // TEMPORARY DEBUG: log every response's stop_reason/id/raw text so we can tell a
    // real refusal apart from a schema-valid-but-content-empty ("placeholder") reply -
    // neither of the existing log lines (logRefusal, the isEmpty() branch below) fire
    // for the latter, since the response is a normal 200 with well-formed JSON content.
    // max_tokens responses are logged with just the last 100 chars: a runaway repetition
    // loop that fills the whole token budget produces tens of KB of near-identical text,
    // and only the tail (what it got stuck repeating) is useful for diagnosis.
    // LOG.info("Claude response received. id=" + response.id() +
    //     ", model=" + response.model() +
    //     ", stopReason=" + response.stopReason().map(Object::toString).orElse("<none>") +
    //     ", stopDetails=" + response.stopDetails().map(Object::toString).orElse("<none>") +
    //     ", rawText=" + (isMaxTokens(response) ? lastChars(rawText.orElse("<empty>"), 100) : rawText.orElse("<empty>")));

    if (rawText.isEmpty()) {
      LOG.error("No text content found in Claude response. id=" + response.id() +
          ", model=" + response.model() +
          ", stopReason=" + response.stopReason().map(Object::toString).orElse("<none>") +
          ", stopDetails=" + response.stopDetails().map(Object::toString).orElse("<none>") +
          ", usage=" + response.usage() +
          ", full response=" + response);
    }

    return rawText
        .orElseThrow(() -> new RuntimeException("No text content found in Claude response"))
        .trim();
  }

  @Override
  protected void updateCostMonitor(Object apiResponse) {
    // Claude response handling is done in callApiForJson
  }
}
