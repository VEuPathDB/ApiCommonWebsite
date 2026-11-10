package org.apidb.apicommon.service.services.jbrowse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The JBrowse 2 config a page loads: the project's root config (assemblies,
 * connections, ...) plus the plugins of the installed JBrowse 2 artifact.
 * Plugins belong to the artifact alone, so a root that declares any is refused.
 */
public final class JBrowse2RootConfig {

  public static final String PLUGINS = "plugins";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JBrowse2RootConfig() {}

  public static ObjectNode merge(Optional<ObjectNode> root, String rootName,
      ObjectNode artifact, String contextPath) {

    ObjectNode out = MAPPER.createObjectNode();
    if (root.isPresent()) {
      if (root.get().has(PLUGINS)) {
        throw new IllegalArgumentException("JBrowse 2 root config " + rootName +
            " declares '" + PLUGINS + "'; plugins come only from the installed artifact's config.json");
      }
      out.setAll(root.get());
    }

    ArrayNode plugins = out.putArray(PLUGINS);
    JsonNode artifactPlugins = artifact.path(PLUGINS);
    for (JsonNode plugin : artifactPlugins) {
      ObjectNode copy = plugin.deepCopy();
      JsonNode url = copy.get("url");
      if (url != null && url.isTextual()) {
        copy.put("url", resolvePluginUrl(url.asText(), contextPath));
      }
      plugins.add(copy);
    }
    return out;
  }

  static String resolvePluginUrl(String url, String contextPath) {
    if (isAbsolute(url)) {
      return url;
    }
    return contextPath + "/jbrowse2/" + url;
  }

  private static boolean isAbsolute(String url) {
    if (url.startsWith("/")) {
      return true;
    }
    try {
      return new URI(url).isAbsolute();
    }
    catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * If-None-Match test that tolerates the "-gzip"/"-br" suffix Apache's
   * mod_deflate/mod_brotli append to an ETag they compress, which the
   * client then echoes back.
   */
  public static boolean etagMatches(String ifNoneMatch, String etag) {
    if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
      return false;
    }
    for (String candidate : ifNoneMatch.split(",")) {
      String tag = candidate.trim();
      if (tag.equals("*")) {
        return true;
      }
      if (tag.startsWith("W/")) {
        tag = tag.substring(2);
      }
      tag = tag.replaceAll("^\"|\"$", "").replaceAll("-(gzip|br)$", "");
      if (tag.equals(etag)) {
        return true;
      }
    }
    return false;
  }

  public static Optional<ObjectNode> readOptional(Path file) throws IOException {
    try {
      return Optional.of(readObject(file));
    }
    catch (NoSuchFileException e) {
      return Optional.empty();
    }
  }

  public static ObjectNode readObject(Path file) throws IOException {
    JsonNode node = MAPPER.readTree(Files.readAllBytes(file));
    if (node == null || !node.isObject()) {
      throw new IOException("JBrowse 2 config " + file + " is not a JSON object");
    }
    return (ObjectNode) node;
  }

  public static String serialize(ObjectNode config) throws IOException {
    return MAPPER.writeValueAsString(config);
  }
}
