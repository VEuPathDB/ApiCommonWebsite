/**
 * 
 */
package org.apidb.apicomment.model;

import java.io.IOException;

import org.apidb.apicommon.model.CommentConfig;
import org.apidb.apicommon.model.CommentConfigParser;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.*;

/**
 * @author xingao
 * 
 */
public class CommentConfigTest {

    @Test
    public void testParseConfig() throws SAXException, IOException,
            WdkModelException {
        String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);
        String projectId = System.getProperty(Utilities.SYSTEM_PROPERTY_PROJECT_ID);

        CommentConfigParser parser = new CommentConfigParser(gusHome);
        CommentConfig config = parser.parseConfig(projectId);

        // validate the content of the the config
        assertTrue("commentTextFileDir",
                config.getCommentTextFileDir().length() > 0);
        assertTrue("connectionUrl", config.getConnectionUrl().length() > 0);
        assertTrue("login", config.getLogin().length() > 0);
        assertTrue("password", config.getPassword().length() > 0);
        assertTrue("platformClass", config.getPlatformClass().length() > 0);
        assertTrue("minIdle", config.getMinIdle() >= 0);
        assertTrue("maxIdle", config.getMaxIdle() >= config.getMinIdle());
        assertTrue("maxActive", config.getMaxActive() > 0);
        assertTrue("maxWait", config.getMaxWait() >= 0);
        assertTrue("initialSize", config.getInitialSize() >= 0);
    }
}
