package org.apidb.apicommon.model.report.ai.expression;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema.Schema;

public class ClaudeSummarizer extends Summarizer {

  public static final Model CLAUDE_MODEL = Model.CLAUDE_SONNET_4_20250514;

  private static final String CLAUDE_API_KEY_PROP_NAME = "CLAUDE_API_KEY";

  private final AnthropicClientAsync _claudeClient;

  public ClaudeSummarizer(WdkModel wdkModel, DailyCostMonitor costMonitor) throws WdkModelException {
    super(costMonitor);

    String apiKey = wdkModel.getProperties().get(CLAUDE_API_KEY_PROP_NAME);
    if (apiKey == null) {
      throw new WdkModelException("WDK property '" + CLAUDE_API_KEY_PROP_NAME + "' has not been set.");
    }

    _claudeClient = AnthropicOkHttpClientAsync.builder()
        .apiKey(apiKey)
        .maxRetries(32)  // Handle 429 errors
        .checkJacksonVersionCompatibility(false)
        .build();
  }

  @Override
  protected CompletableFuture<String> callApiForJson(String prompt, Schema schema) {
    // Convert JSON schema to natural language description for Claude
    String jsonFormatInstructions = convertSchemaToPromptInstructions(schema);
    
    String enhancedPrompt = prompt + "\n\n" + jsonFormatInstructions;

    MessageCreateParams request = MessageCreateParams.builder()
        .model(CLAUDE_MODEL)
        .maxTokens((long) MAX_RESPONSE_TOKENS)
        .system(SYSTEM_MESSAGE)
        .addUserMessage(enhancedPrompt)
        .build();

    return _claudeClient.messages().create(request).thenApply(response -> {
      // Convert Claude usage to OpenAI format for cost monitoring
      com.anthropic.models.messages.Usage claudeUsage = response.usage();
      com.openai.models.CompletionUsage openAiUsage = com.openai.models.CompletionUsage.builder()
          .promptTokens(claudeUsage.inputTokens())
          .completionTokens(claudeUsage.outputTokens())
          .totalTokens(claudeUsage.inputTokens() + claudeUsage.outputTokens())
          .build();
      
      _costMonitor.updateCost(java.util.Optional.of(openAiUsage));
      
      // Extract text from content blocks using stream API
      String rawText = response.content().stream()
          .flatMap(contentBlock -> contentBlock.text().stream())
          .map(textBlock -> textBlock.text())
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No text content found in Claude response"));
      
      // Strip JSON markdown formatting if present
      return stripJsonMarkdown(rawText);
    });
  }

  @Override
  protected void updateCostMonitor(Object apiResponse) {
    // Claude response handling is done in callApiForJson
  }

  private String stripJsonMarkdown(String text) {
    String trimmed = text.trim();
    
    // Remove ```json and ``` markdown formatting
    if (trimmed.startsWith("```json")) {
      trimmed = trimmed.substring(7); // Remove "```json"
    } else if (trimmed.startsWith("```")) {
      trimmed = trimmed.substring(3); // Remove "```"
    }
    
    if (trimmed.endsWith("```")) {
      trimmed = trimmed.substring(0, trimmed.length() - 3); // Remove trailing "```"
    }
    
    return trimmed.trim();
  }

  private String convertSchemaToPromptInstructions(Schema schema) {
    // Convert OpenAI JSON schema to Claude-friendly format instructions
    if (schema == experimentResponseSchema) {
      return "Respond in valid JSON format matching this exact structure:\n" +
          "{\n" +
          "  \"one_sentence_summary\": \"string describing gene expression\",\n" +
          "  \"biological_importance\": \"integer 0-5\",\n" +
          "  \"confidence\": \"integer 0-5\",\n" +
          "  \"experiment_keywords\": [\"array\", \"of\", \"strings\"],\n" +
          "  \"notes\": \"string with additional context\"\n" +
          "}";
    } else if (schema == finalResponseSchema) {
      return "Respond in valid JSON format matching this exact structure:\n" +
          "{\n" +
          "  \"headline\": \"string summarizing key results\",\n" +
          "  \"one_paragraph_summary\": \"string with ~100 words\",\n" +
          "  \"topics\": [\n" +
          "    {\n" +
          "      \"headline\": \"string summarizing topic\",\n" +
          "      \"one_sentence_summary\": \"string describing topic results\",\n" +
          "      \"dataset_ids\": [\"array\", \"of\", \"dataset\", \"strings\"]\n" +
          "    }\n" +
          "  ]\n" +
          "}";
    } else {
      return "Respond in valid JSON format.";
    }
  }
}
