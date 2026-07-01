package org.apidb.apicommon.service.services.ai.llm;

import java.util.List;

import org.gusdb.wdk.model.WdkModelException;

/**
 * The single raw Anthropic interaction the {@link AnthropicJsonClient} depends
 * on: given a system prompt, an ordered list of user-message turns, and a JSON
 * schema, return the model's completion constrained to that schema via
 * structured outputs ({@code output_config.format}). Isolating this as a seam
 * lets the JSON-extraction and formatter-retry orchestration be unit-tested
 * without any network access.
 */
@FunctionalInterface
public interface LlmCompleter {

  String complete(String system, List<String> userPrompts, String jsonSchema) throws WdkModelException;
}
