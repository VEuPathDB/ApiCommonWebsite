package org.apidb.apicommon.service.services.jbrowse;

import static org.gusdb.fgputil.functional.Functions.getMapFromList;
import static org.gusdb.fgputil.xml.XmlParser.configureNode;
import static org.gusdb.fgputil.xml.XmlParser.makeURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.xml.XmlValidator;
import org.gusdb.wdk.model.WdkModelText;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.xml.sax.SAXException;

/**
 * Static class that loads JBrowse-related SQL queries from XML configuration
 * files on class load and keeps them in memory for efficient access.
 * 
 * @author rdoherty
 */
public class JBrowseQueries {

  private static final String RNG_LOCATION = "/lib/rng/jbrowseQueries.rng";

  private static final String XML_FILES_LOCATION = "/lib/xml/jbrowse";

  private static final String[] XML_FILES = { "genomeQueries.xml", "proteinQueries.xml" };

  private static Map<String,String> _queryMap;

  public static Map<String,String> getQueryMap() {
    if (_queryMap == null) {
      assignQueryMap();
    }
    return _queryMap;
  }

  private static synchronized void assignQueryMap() {
    if (_queryMap == null) {
      _queryMap = loadQueryMap();
    }
  }

  private static Map<String,String> loadQueryMap() {
    try {
      URL rngFileUrl = makeURL(Paths.get(GusHome.getGusHome(), RNG_LOCATION).toString());
      XmlValidator validator = new XmlValidator(rngFileUrl);
      Digester digester = configureDigester();
      Map<String,String> aggregateMap = new HashMap<>();
      for (String xmlFileName : XML_FILES) {
        URL xmlFileUrl = makeURL(Paths.get(GusHome.getGusHome(), XML_FILES_LOCATION, xmlFileName).toString());
        if (!validator.validate(xmlFileUrl)) {
          throw new WdkRuntimeException("RNG Validation failed for file: " + xmlFileUrl.toExternalForm());
        }
        @SuppressWarnings("unchecked")
        List<WdkModelText> queries = (List<WdkModelText>)digester.parse(xmlFileUrl.openStream());
        aggregateMap.putAll(getMapFromList(queries, query -> new TwoTuple<>(query.getName(), query.getText())));
      }
      return aggregateMap;
    }
    catch (IOException | SAXException e) {
      throw new WdkRuntimeException("Unable to load JBrowse queries", e);
    }
  }

  private static Digester configureDigester() {
    Digester digester = new Digester();
    digester.setValidating(false);
    digester.addObjectCreate("jbrowse", ArrayList.class);
    configureNode(digester, "jbrowse/query", WdkModelText.class, "add");
    digester.addCallMethod("jbrowse/query", "setText", 0);
    return digester;
  }

  public static void main(String[] args) {
    System.out.println(FormatUtil.prettyPrint(getQueryMap(), Style.MULTI_LINE));
  }

}
