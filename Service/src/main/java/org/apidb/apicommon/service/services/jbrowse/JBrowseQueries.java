package org.apidb.apicommon.service.services.jbrowse;

import static java.util.stream.Collectors.toMap;
import static org.gusdb.fgputil.xml.XmlParser.configureNode;
import static org.gusdb.fgputil.xml.XmlParser.makeURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
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

  private static List<WdkModelText> _queryList;

  public static Map<String,String> getQueryMap(String projectId) {
    if (_queryList == null) {
      synchronized(JBrowseQueries.class) {
        if (_queryList == null) {
          _queryList = loadQueryList();
        }
      }
    }
    return _queryList.stream()
      .filter(entry -> entry.include(projectId))
      .collect(toMap(WdkModelText::getName, WdkModelText::getText));
  }

  private static List<WdkModelText> loadQueryList() {
    try {
      URL rngFileUrl = makeURL(Paths.get(GusHome.getGusHome(), RNG_LOCATION).toString());
      XmlValidator validator = new XmlValidator(rngFileUrl);
      Digester digester = configureDigester();
      List<WdkModelText> queryList = new ArrayList<>();
      for (String xmlFileName : XML_FILES) {
        URL xmlFileUrl = makeURL(Paths.get(GusHome.getGusHome(), XML_FILES_LOCATION, xmlFileName).toString());
        if (!validator.validate(xmlFileUrl)) {
          throw new WdkRuntimeException("RNG Validation failed for file: " + xmlFileUrl.toExternalForm());
        }
        @SuppressWarnings("unchecked")
        List<WdkModelText> queries = (List<WdkModelText>)digester.parse(xmlFileUrl.openStream());
        queryList.addAll(queries);
      }
      return queryList;
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
    if (args.length != 1) {
      System.err.println("USAGE: fgpJava " + JBrowseQueries.class.getName() + " <projectId>");
      System.exit(1);
    }
    System.out.println(FormatUtil.prettyPrint(getQueryMap(args[0]), Style.MULTI_LINE));
  }

}
