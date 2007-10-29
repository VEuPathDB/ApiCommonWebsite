/**
 * 
 */
package org.apidb.apicommon.model;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.gusdb.wdk.model.ModelConfig;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.XmlParser;
import org.xml.sax.SAXException;

/**
 * @author xingao
 * 
 */
public class CommentConfigParser extends XmlParser {

    public CommentConfigParser(String gusHome) throws SAXException, IOException {
        super(gusHome, "lib/rng/comment-config.rng");
    }

    /**
     * @param projectId
     * @return the relative path from $GUS_HOME to the config file
     */
    public String getConfigFile(String projectId) {
        return "config/" + projectId + "/comment-config.xml";
    }

    public CommentConfig parseConfig(String projectId) throws SAXException,
            IOException, WdkModelException {
        // validate the configuration file
        URL configURL = makeURL(gusHome, getConfigFile(projectId));
        if (!validate(configURL))
            throw new WdkModelException("Relax-NG validation failed on "
                    + configURL.toExternalForm());

        return (CommentConfig) digester.parse(configURL.openStream());
    }

    protected Digester configureDigester() {
        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("commentConfig", CommentConfig.class);
        digester.addSetProperties("commentConfig");

        return digester;
    }
}
