package org.apidb.apicommon.service.services.jbrowse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JBrowse2RootConfigTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String CTX = "/plasmo.dev";

  private static ObjectNode json(String s) throws Exception {
    return (ObjectNode) MAPPER.readTree(s.replace('\'', '"'));
  }

  private static final String ARTIFACT =
      "{'plugins':[{'name':'A','url':'plugins/a.js'},{'name':'B','url':'https://cdn.example/b.js'}]}";

  @Test
  public void rootKeysArePreservedAndPluginsAppended() throws Exception {
    ObjectNode root = json("{'assemblies':[{'name':'x'}],'connections':[]}");
    ObjectNode out = JBrowse2RootConfig.merge(Optional.of(root), "root.json", json(ARTIFACT), CTX);

    List<String> keys = new ArrayList<>();
    out.fieldNames().forEachRemaining(keys::add);
    assertEquals(List.of("assemblies", "connections", "plugins"), keys);
    assertEquals(root.get("assemblies"), out.get("assemblies"));
    assertEquals(2, out.get("plugins").size());
    assertFalse("root must not be mutated", root.has("plugins"));
  }

  @Test
  public void noRootYieldsPluginsOnly() throws Exception {
    ObjectNode out = JBrowse2RootConfig.merge(Optional.empty(), "root.json", json(ARTIFACT), CTX);
    List<String> keys = new ArrayList<>();
    out.fieldNames().forEachRemaining(keys::add);
    assertEquals(List.of("plugins"), keys);
    assertEquals("A", out.get("plugins").get(0).get("name").asText());
  }

  @Test
  public void rootWithPluginsIsRefusedNamingTheFile() throws Exception {
    ObjectNode root = json("{'assemblies':[],'plugins':[]}");
    try {
      JBrowse2RootConfig.merge(Optional.of(root), "/mirror/jbrowse2/config.json", json(ARTIFACT), CTX);
      fail("expected refusal");
    }
    catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("/mirror/jbrowse2/config.json"));
    }
  }

  @Test
  public void relativeUrlIsResolvedUnderTheArtifact() throws Exception {
    ObjectNode out = JBrowse2RootConfig.merge(Optional.empty(), "r", json(ARTIFACT), CTX);
    assertEquals("/plasmo.dev/jbrowse2/plugins/a.js", out.get("plugins").get(0).get("url").asText());
  }

  @Test
  public void absoluteUrlsAreUnchanged() throws Exception {
    ObjectNode out = JBrowse2RootConfig.merge(Optional.empty(), "r", json(ARTIFACT), CTX);
    assertEquals("https://cdn.example/b.js", out.get("plugins").get(1).get("url").asText());
    assertEquals("/elsewhere/c.js", JBrowse2RootConfig.resolvePluginUrl("/elsewhere/c.js", CTX));
  }

  @Test
  public void etagMatchesToleratesCompressionSuffixes() {
    assertTrue(JBrowse2RootConfig.etagMatches("\"abc\"", "abc"));
    assertTrue(JBrowse2RootConfig.etagMatches("\"abc-br\"", "abc"));
    assertTrue(JBrowse2RootConfig.etagMatches("W/\"abc-gzip\"", "abc"));
    assertTrue(JBrowse2RootConfig.etagMatches("\"x\", \"abc-br\"", "abc"));
    assertTrue(JBrowse2RootConfig.etagMatches("*", "abc"));
    assertFalse(JBrowse2RootConfig.etagMatches("\"abd-br\"", "abc"));
    assertFalse(JBrowse2RootConfig.etagMatches(null, "abc"));
  }

  @Test
  public void otherPluginFieldsAreKept() throws Exception {
    ObjectNode artifact = json("{'plugins':[{'name':'A','url':'a.js','extra':1}]}");
    ObjectNode out = JBrowse2RootConfig.merge(Optional.empty(), "r", artifact, CTX);
    assertEquals(1, out.get("plugins").get(0).get("extra").asInt());
    assertEquals("a.js", artifact.get("plugins").get(0).get("url").asText());
  }
}
