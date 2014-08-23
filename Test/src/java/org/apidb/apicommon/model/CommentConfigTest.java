/**
 * 
 */
package org.apidb.apicommon.model;

import static org.junit.Assert.assertTrue;

import org.apidb.apicommon.model.comment.CommentConfig;
import org.apidb.apicommon.model.comment.CommentConfigParser;
import org.apidb.apicommon.model.comment.CommentModelException;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;

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
    assertTrue("connectionUrl", config.getConnectionUrl().length() > 0);
    assertTrue("login", config.getLogin().length() > 0);
    assertTrue("password", config.getPassword().length() > 0);
    assertTrue("platformClass", config.getPlatform() != null);
    assertTrue("minIdle", config.getMinIdle() >= 0);
    assertTrue("maxIdle", config.getMaxIdle() >= config.getMinIdle());
    assertTrue("maxActive", config.getMaxActive() > 0);
    assertTrue("maxWait", config.getMaxWait() >= 0);
  }
}
