package org.apidb.apicommon.service.services.ai.llm;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The JSON-producing prompt call the pipeline depends on: render the named stage
 * with the given placeholder replacements, call the LLM, and return the parsed
 * JSON. {@link AnthropicJsonClient} is the production implementation; tests
 * inject a fake to keep the pipeline's stage orchestration network-free.
 */
@FunctionalInterface
public interface JsonPromptClient {

  JsonNode complete(String stage, Map<String, String> replacements) throws WdkModelException;
}
