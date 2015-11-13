package org.apidb.apicommon.model.comment;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.gusdb.fgputil.xml.XmlParser;
import org.gusdb.fgputil.xml.XmlValidator;
import org.gusdb.wdk.model.WdkModelException;
import org.xml.sax.SAXException;

/**
 * @author xingao
 */
public class CommentConfigParser extends XmlParser {

  private final String _gusHome;
  private final Digester _digester;

  public CommentConfigParser(String gusHome) {
    _gusHome = gusHome;
    _digester = configureDigester();
  }

  public CommentConfig parseConfig(String projectId) throws WdkModelException, CommentModelException {
    try {
      // validate the configuration file
      XmlValidator validator = new XmlValidator(_gusHome + "/lib/rng/comment-config.rng");
      URL configUrl = makeURL(_gusHome + "/config/" + projectId + "/comment-config.xml");
      if (!validator.validate(configUrl)) {
        throw new WdkModelException("Validation failed: " + configUrl.toExternalForm());
      }
      return (CommentConfig) _digester.parse(configUrl.openStream());
    }
    catch (IOException | SAXException ex) {
      throw new CommentModelException(ex);
    }
  }

  private static Digester configureDigester() {
    Digester digester = new Digester();
    digester.setValidating(false);

    digester.addObjectCreate("commentConfig", CommentConfig.class);
    digester.addSetProperties("commentConfig");

    return digester;
  }
}
