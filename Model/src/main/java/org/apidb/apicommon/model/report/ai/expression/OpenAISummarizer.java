package org.apidb.apicommon.model.report.ai.expression;

import java.util.concurrent.CompletableFuture;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema;

public class OpenAISummarizer extends Summarizer {

  // provide exact model number for semi-reproducibility
  public static final ChatModel OPENAI_CHAT_MODEL = ChatModel.GPT_4O_2024_11_20; // GPT_4O_2024_08_06;

  private static final String OPENAI_API_KEY_PROP_NAME = "OPENAI_API_KEY";

  private final OpenAIClientAsync _openAIClient;

  public OpenAISummarizer(WdkModel wdkModel, DailyCostMonitor costMonitor) throws WdkModelException {
    super(costMonitor);

    String apiKey = wdkModel.getProperties().get(OPENAI_API_KEY_PROP_NAME);
    if (apiKey == null) {
      throw new WdkModelException("WDK property '" + OPENAI_API_KEY_PROP_NAME + "' has not been set.");
    }

    _openAIClient = OpenAIOkHttpClientAsync.builder()
        .apiKey(apiKey)
        .maxRetries(32)  // Handle 429 errors
        .build();
  }

  @Override
  protected CompletableFuture<String> callApiForJson(String prompt, com.openai.models.ResponseFormatJsonSchema.JsonSchema.Schema schema) {
    ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
        .model(OPENAI_CHAT_MODEL)
        .maxCompletionTokens(MAX_RESPONSE_TOKENS)
        .responseFormat(ResponseFormatJsonSchema.builder()
            .jsonSchema(JsonSchema.builder()
                .name("structured-response")
                .schema(schema)
                .strict(true)
                .build())
            .build())
        .addSystemMessage(SYSTEM_MESSAGE)
        .addUserMessage(prompt)
        .build();

    return _openAIClient.chat().completions().create(request).thenApply(completion -> {
      // update cost accumulator
      _costMonitor.updateCost(completion.usage());
      
      // return JSON string
      return completion.choices().get(0).message().content().get();
    });
  }

  @Override
  protected void updateCostMonitor(Object apiResponse) {
    // OpenAI response handling is done in callApiForJson
  }
}