package org.apidb.apicommon.model;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.gusdb.fgputil.functional.Functions.getMapFromKeys;
import static org.gusdb.fgputil.xml.XmlParser.configureNode;
import static org.gusdb.fgputil.xml.XmlParser.makeURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester3.Digester;
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

  private static final String RNG_FILE_LOCATION = "/lib/rng/jbrowseQueries.rng";
  private static final String XML_FILE_LOCATION = "/lib/xml/jbrowse";

  public enum Category {
    GENOME("genomeQueries.xml"),
    PROTEIN("proteinQueries.xml");

    private String _sourceFile;

    private Category(String sourceFile) {
      _sourceFile = sourceFile;
    }

    public String getSourceFile() {
      return _sourceFile;
    }
  }

  private static Map<Category,List<WdkModelText>> _queries;

  /**
   * @param projectId project ID of queries to include
   * @param category category of queries to return
   * @return a map from query name to its SQL
   */
  public static Map<String,String> getQueryMap(String projectId, Category category) {
    if (_queries == null) {
      synchronized(JBrowseQueries.class) {
        if (_queries == null) {
          _queries = loadQueries();
        }
      }
    }
    return _queries.get(category).stream()
      .filter(entry -> entry.include(projectId))
      .collect(toMap(WdkModelText::getName, WdkModelText::getText));
  }

  /**
   * @param projectId project ID of queries to include
   * @return all JBrowse query maps by category
   */
  public static Map<Category, Map<String, String>> getComprehensiveQueryMap(String projectId) {
    return getMapFromKeys(asList(Category.values()), category -> getQueryMap(projectId, category));
  }

  private static Map<Category,List<WdkModelText>> loadQueries() {
    try {
      URL rngFileUrl = makeURL(Paths.get(GusHome.getGusHome(), RNG_FILE_LOCATION).toString());
      XmlValidator validator = new XmlValidator(rngFileUrl);
      Digester digester = configureDigester();
      Map<Category,List<WdkModelText>> queryMap = new HashMap<>();
      for (Category category : Category.values()) {
        URL xmlFileUrl = makeURL(Paths.get(GusHome.getGusHome(), XML_FILE_LOCATION, category.getSourceFile()).toString());
        if (!validator.validate(xmlFileUrl)) {
          throw new WdkRuntimeException("RNG Validation failed for file: " + xmlFileUrl.toExternalForm());
        }
        @SuppressWarnings("unchecked")
        List<WdkModelText> queries = (List<WdkModelText>)digester.parse(xmlFileUrl.openStream());
        queryMap.put(category, queries);
      }
      return queryMap;
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
    for (Category category : Category.values()) {
      System.out.println("Category: " + category + FormatUtil.NL +
          FormatUtil.prettyPrint(getQueryMap(args[0], category), Style.MULTI_LINE));
    }
  }

}
