package org.apidb.apicommon.service.services.ai.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for {@link AnthropicJsonClient}: the pure {@code extractJson} fence-strip
 * + parse (port of Python {@code extract_json}), and the formatter-retry loop
 * orchestration (port of the STEP_1 retry loop) exercised through an injected
 * {@link LlmCompleter} so no network is touched.
 */
public class AnthropicJsonClientTest {

  // --- extractJson ----------------------------------------------------------

  @Test
  public void extractJsonParsesPlainObject() {
    JsonNode n = AnthropicJsonClient.extractJson("{\"only_in_passing\": false}");
    assertEquals(false, n.get("only_in_passing").asBoolean());
  }

  @Test
  public void extractJsonStripsJsonFences() {
    JsonNode n = AnthropicJsonClient.extractJson("```json\n{\"a\": 1}\n```");
    assertEquals(1, n.get("a").asInt());
  }

  @Test
  public void extractJsonStripsPlainFences() {
    JsonNode n = AnthropicJsonClient.extractJson("```\n{\"a\": 2}\n```");
    assertEquals(2, n.get("a").asInt());
  }

  @Test
  public void extractJsonToleratesSurroundingWhitespace() {
    JsonNode n = AnthropicJsonClient.extractJson("   \n {\"a\": 3}  \n ");
    assertEquals(3, n.get("a").asInt());
  }

  @Test
  public void extractJsonReturnsNullOnUnparseableText() {
    assertNull(AnthropicJsonClient.extractJson("this is not json at all"));
  }

  // --- formatter retry loop -------------------------------------------------

  /** Records calls and replays a fixed queue of canned completions. */
  private static final class FakeCompleter implements LlmCompleter {
    final Deque<String> responses = new ArrayDeque<>();
    int calls = 0;

    FakeCompleter(String... canned) {
      Collections.addAll(responses, canned);
    }

    @Override
    public String complete(String system, List<String> userPrompts, String prefill) {
      calls++;
      return responses.isEmpty() ? "still broken" : responses.poll();
    }
  }

  private static AnthropicJsonClient client(FakeCompleter completer) {
    return new AnthropicJsonClient(completer, new PromptLoader());
  }

  @Test
  public void completeReturnsParsedJsonWithoutRetryWhenFirstResponseIsValid() throws Exception {
    FakeCompleter completer = new FakeCompleter("{\"only_in_passing\": true}");
    JsonNode out = client(completer).complete("getGeneSummary", Collections.emptyMap());

    assertEquals(true, out.get("only_in_passing").asBoolean());
    assertEquals("a valid first response needs no formatter call", 1, completer.calls);
  }

  @Test
  public void completeRecoversViaFormatterRetryAfterMalformedResponse() throws Exception {
    FakeCompleter completer = new FakeCompleter("not json", "{\"recovered\": true}");
    JsonNode out = client(completer).complete("getGeneSummary", Collections.emptyMap());

    assertEquals(true, out.get("recovered").asBoolean());
    assertEquals("one summary call + one formatter retry", 2, completer.calls);
  }

  @Test
  public void completeThrowsAfterExhaustingRetries() {
    // 1 summary call + MAX_RETRY formatter calls all malformed
    FakeCompleter completer = new FakeCompleter("bad", "bad", "bad", "bad");
    try {
      client(completer).complete("getGeneSummary", Collections.emptyMap());
      fail("expected WdkModelException after exhausting retries");
    }
    catch (WdkModelException expected) {
      assertTrue(expected.getMessage().toLowerCase().contains("retr"));
    }
    assertEquals("1 initial + MAX_RETRY formatter attempts",
        1 + AnthropicJsonClient.MAX_RETRY, completer.calls);
  }
}
