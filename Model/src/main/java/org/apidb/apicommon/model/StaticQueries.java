package org.apidb.apicommon.model;

import static java.util.stream.Collectors.toMap;
import static org.gusdb.fgputil.xml.XmlParser.configureNode;
import static org.gusdb.fgputil.xml.XmlParser.makeURL;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.digester3.Digester;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.FormatUtil.Style;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.fgputil.xml.XmlValidator;
import org.gusdb.wdk.model.WdkModelText;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.xml.sax.SAXException;

public class StaticQueries {

  private static final String RNG_FILE_LOCATION = "/lib/rng/staticQueries.rng";

  public interface SourceFileProvider {
    String getSourceFile();
  }

  public static class QueryCache<T extends SourceFileProvider> extends HashMap<T,List<WdkModelText>> { }

  public static <T extends SourceFileProvider> Map<String, String> filterByProject(List<WdkModelText> queries, String projectId) {
    return queries.stream()
      .filter(entry -> entry.include(projectId))
      .collect(toMap(WdkModelText::getName, WdkModelText::getText));
  }

  public static <T extends SourceFileProvider> QueryCache<T> loadQueries(T[] allProviders) {
    try {
      URL rngFileUrl = makeURL(Paths.get(GusHome.getGusHome(), RNG_FILE_LOCATION).toString());
      XmlValidator validator = new XmlValidator(rngFileUrl);
      Digester digester = configureDigester();
      QueryCache<T> queryMap = new QueryCache<T>();
      for (T source : allProviders) {
        URL xmlFileUrl = makeURL(Paths.get(GusHome.getGusHome(), source.getSourceFile()).toString());
        if (!validator.validate(xmlFileUrl)) {
          throw new WdkRuntimeException("RNG Validation failed for file: " + xmlFileUrl.toExternalForm());
        }
        @SuppressWarnings("unchecked")
        List<WdkModelText> queries = (List<WdkModelText>)digester.parse(xmlFileUrl.openStream());
        queryMap.put(source, queries);
      }
      return queryMap;
    }
    catch (IOException | SAXException e) {
      throw new WdkRuntimeException("Unable to load static queries", e);
    }
  }

  private static Digester configureDigester() {
    Digester digester = new Digester();
    digester.setValidating(false);
    digester.addObjectCreate("staticQueries", ArrayList.class);
    configureNode(digester, "staticQueries/query", WdkModelText.class, "add");
    digester.addCallMethod("staticQueries/query", "setText", 0);
    return digester;
  }

  public static <T extends SourceFileProvider> void doMain(String[] args,
      Class<?> clazz, T[] sources, BiFunction<String,T,Map<String,String>> queryMapProvider) {
    if (args.length != 1) {
      System.err.println("USAGE: fgpJava " + clazz.getName() + " <projectId>");
      System.exit(1);
    }
    for (T source : sources) {
      System.out.println("Source: " + source.getSourceFile() + FormatUtil.NL +
          FormatUtil.prettyPrint(queryMapProvider.apply(args[0], source), Style.MULTI_LINE));
    }
  }

}
