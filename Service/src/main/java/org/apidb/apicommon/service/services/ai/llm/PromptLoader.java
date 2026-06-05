package org.apidb.apicommon.service.services.ai.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the prompt resource files for a stage from the classpath
 * ({@code ai/prompts/<stage>/{system.txt,user.txt,schema.json}}) and performs
 * naive {@code [PLACEHOLDER]} substitution — mirroring the Python pipeline's
 * {@code get_prompt_and_replace} (simple {@code str.replace}). Files are read
 * once and cached.
 *
 * <p>{@code user.txt} holds one or more user-message turns separated by a blank
 * line (the Anthropic SDK gets one {@code addUserMessage} per turn). Splitting
 * into turns happens on the raw template, <em>before</em> placeholder
 * substitution, so a substituted value (e.g. paper text containing blank lines)
 * can never be mistaken for a turn boundary.
 */
public class PromptLoader {

  private static final String BASE = "ai/prompts/";

  /** Blank-line separator between user turns (one or more whitespace-only lines). */
  private static final String TURN_SEPARATOR = "\\r?\\n[ \\t]*\\r?\\n";

  private final Map<String, String> _cache = new ConcurrentHashMap<>();

  /** Raw {@code system.txt} for the stage (placeholders not yet substituted). */
  public String system(String stage) {
    return load(stage, "system.txt");
  }

  /** Raw {@code user.txt} turns for the stage, split on blank lines. */
  public List<String> userTurns(String stage) {
    String raw = load(stage, "user.txt").stripTrailing();
    return Arrays.asList(raw.split(TURN_SEPARATOR));
  }

  /** Raw {@code schema.json} text for the stage (substituted into {@code [JSON_SCHEMA]}). */
  public String schema(String stage) {
    return load(stage, "schema.json");
  }

  /** Replace every {@code [KEY]} marker with its mapped value; unknown markers are left as-is. */
  public static String render(String template, Map<String, String> replacements) {
    String out = template;
    for (Map.Entry<String, String> e : replacements.entrySet()) {
      out = out.replace("[" + e.getKey() + "]", e.getValue());
    }
    return out;
  }

  private String load(String stage, String file) {
    String key = stage + "/" + file;
    return _cache.computeIfAbsent(key, this::readResource);
  }

  private String readResource(String key) {
    String path = BASE + key;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null)
        throw new IllegalStateException("Prompt resource not found on classpath: " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to read prompt resource: " + path, e);
    }
  }
}
