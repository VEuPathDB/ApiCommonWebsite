package org.apidb.apicommon.model;

import static org.apidb.apicommon.model.StaticQueries.doMain;
import static org.apidb.apicommon.model.StaticQueries.filterByProject;
import static org.apidb.apicommon.model.StaticQueries.loadQueries;

import java.util.Map;

import org.apidb.apicommon.model.StaticQueries.QueryCache;
import org.apidb.apicommon.model.StaticQueries.SourceFileProvider;

/**
 * Static class that loads JBrowse-related SQL queries from XML configuration
 * files on class load and keeps them in memory for efficient access.
 * 
 * @author rdoherty
 */
public class JBrowseQueries {

  private static final String XML_FILE_LOCATION = "/lib/xml/jbrowse/";

  public enum Category implements SourceFileProvider {
    GENOME("genomeQueries.xml"),
    PROTEIN("proteinQueries.xml");

    private String _sourceFile;

    private Category(String sourceFile) {
      _sourceFile = sourceFile;
    }

    @Override
    public String getSourceFile() {
      return XML_FILE_LOCATION + _sourceFile;
    }
  }

  private static QueryCache<Category> _queries;

  /**
   * @param projectId project ID of queries to include
   * @param category category of queries to return
   * @return a map from query name to its SQL
   */
  public static Map<String,String> getQueryMap(String projectId, Category category) {
    return filterByProject(preload().get(category), projectId);
  }

  public static QueryCache<Category> preload() {
    if (_queries == null) {
      _queries = loadQueries(Category.values());
    }
    return _queries;
  }

  public static void main(String[] args) {
    doMain(args, JBrowseQueries.class, Category.values(), (p,c) -> getQueryMap(p,c));
  }
}
