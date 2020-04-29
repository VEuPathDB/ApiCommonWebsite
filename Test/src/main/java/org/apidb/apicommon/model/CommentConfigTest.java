package org.apidb.apicommon.model;

import org.apidb.apicommon.model.comment.CommentConfig;
import org.apidb.apicommon.model.comment.CommentConfigParser;
import org.apidb.apicommon.model.comment.CommentModelException;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xingao
 *
 */
public class CommentConfigTest {

  @Test
  public void testParseConfig() throws WdkModelException, CommentModelException {
    String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);
    String projectId = System.getProperty(Utilities.ARGUMENT_PROJECT_ID);

    CommentConfigParser parser = new CommentConfigParser(gusHome);
    CommentConfig config = parser.parseConfig(projectId);

    // validate the content of the the config
    assertTrue(config.getConnectionUrl().length() > 0, "connectionUrl");
    assertTrue(config.getLogin().length() > 0, "login");
    assertTrue(config.getPassword().length() > 0, "password");
    assertNotNull(config.getPlatform(), "platformClass");
    assertTrue(config.getMinIdle() >= 0, "minIdle");
    assertTrue(config.getMaxIdle() >= config.getMinIdle(), "maxIdle");
    assertTrue(config.getMaxActive() > 0, "maxActive");
    assertTrue(config.getMaxWait() >= 0, "maxWait");
  }
}
