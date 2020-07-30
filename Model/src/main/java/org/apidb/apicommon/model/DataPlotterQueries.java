package org.apidb.apicommon.model;

import static org.apidb.apicommon.model.StaticQueries.doMain;
import static org.apidb.apicommon.model.StaticQueries.filterByProject;
import static org.apidb.apicommon.model.StaticQueries.loadQueries;

import java.util.Map;

import org.apidb.apicommon.model.StaticQueries.QueryCache;
import org.apidb.apicommon.model.StaticQueries.SourceFileProvider;

/**
 * Static class that loads DataPlotter-related SQL queries from XML configuration
 * files on class load and keeps them in memory for efficient access.
 * 
 * @author dcallan
 */
public class DataPlotterQueries {

  private static final String XML_FILE = "/lib/xml/dataPlotter/queries.xml";

  private static final SourceFileProvider ALL_QUERIES = () -> XML_FILE;
  private static final SourceFileProvider[] ALL_QUERIES_ARRAY = new SourceFileProvider[]{ ALL_QUERIES };

  private static QueryCache<SourceFileProvider> _queries;

  /**
   * @param projectId project ID of queries to include
   * @return a map from query name to its SQL
   */
  public static Map<String,String> getQueryMap(String projectId) {
    return filterByProject(preload().get(ALL_QUERIES), projectId);
  }

  public static QueryCache<SourceFileProvider> preload() {
    if (_queries == null) {
      _queries = loadQueries(ALL_QUERIES_ARRAY);
    }
    return _queries;
  }

  public static void main(String[] args) {
    doMain(args, DataPlotterQueries.class, ALL_QUERIES_ARRAY, (p,c) -> getQueryMap(p));
  }
}
