package org.apidb.apicommon.service.services.ai.llm;

import java.util.Map;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Thin wrapper around the Anthropic API for the JSON-producing prompt stages
 * ({@code getGeneSummary}, {@code verifyGeneSummary}). Responsibilities:
 * load the stage's prompt resource files, substitute {@code [PLACEHOLDER]}
 * markers, call the API (prefilling the assistant turn with {@code "{"}),
 * strip any markdown fences, and parse the JSON — retrying via a formatter LLM
 * up to {@code max_retry=3} times on malformed output (port of Python
 * {@code extract_json}).
 *
 * <p>Client setup mirrors {@code ClaudeSummarizer}
 * ({@code AnthropicOkHttpClientAsync.builder().apiKey(...)}). The shared
 * {@code Summarizer} and its disk cache are intentionally NOT reused.
 */
public class AnthropicJsonClient {

  public static final String CLAUDE_API_KEY_PROP_NAME = "CLAUDE_API_KEY";

  private final WdkModel _wdkModel;
  // The Anthropic async client is built (mirroring ClaudeSummarizer) in deliverable 4.

  public AnthropicJsonClient(WdkModel wdkModel) {
    _wdkModel = wdkModel;
  }

  /**
   * Render the named prompt stage with the given placeholder replacements,
   * call the API, and return the parsed JSON response.
   *
   * @param stage        prompt-stage directory name, e.g. {@code getGeneSummary}
   * @param replacements placeholder → value, e.g. {@code [GENE] → "PF3D7_1133400"}
   */
  public JsonNode complete(String stage, Map<String, String> replacements) throws WdkModelException {
    throw new UnsupportedOperationException("AnthropicJsonClient.complete — deliverable 4");
  }
}
