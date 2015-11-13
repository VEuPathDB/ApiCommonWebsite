package org.apidb.apicommon.model.comment;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.gusdb.fgputil.xml.XmlParser;
import org.gusdb.wdk.model.WdkModelException;
import org.xml.sax.SAXException;

/**
 * @author xingao
 * 
 */
public class CommentConfigParser extends XmlParser {

    private final String gusHome;

    public CommentConfigParser(String gusHome) {
        this.gusHome = gusHome;
    }

    /**
     * @param projectId
     * @return the relative path from $GUS_HOME to the config file
     */
    public String getConfigFile(String projectId) {
        return "config/" + projectId + "/comment-config.xml";
    }

    public CommentConfig parseConfig(String projectId) throws WdkModelException, CommentModelException {
        try {
          // validate the configuration file
          configureValidator(gusHome + "/lib/rng/comment-config.rng");
          URL configURL = makeURL(gusHome + "/" + getConfigFile(projectId));
          if (!validate(configURL)) {
            throw new WdkModelException("Validation failed: " + configURL.toExternalForm());
          }
          return (CommentConfig) getDigester().parse(configURL.openStream());
        }
        catch (IOException | SAXException ex) {
          throw new CommentModelException(ex);
        }
    }

    @Override
    protected Digester configureDigester() {
        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("commentConfig", CommentConfig.class);
        digester.addSetProperties("commentConfig");

        return digester;
    }
}
