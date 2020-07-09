package org.eupathdb.sitesearch.data.comments.solr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gusdb.fgputil.SortDirection;

/**
 * Helper for building Solr queries.
 */
public class SolrTermQueryBuilder {

  private static final String
    PARAM_FILTER  = "q",
    PARAM_COLUMNS = "fl",
    PARAM_SORT    = "sort",
    PARAM_ROWS    = "rows",
    PARAM_FORMAT  = "wt";

  private final Map<String, SortDirection> _sort = new HashMap<>();
  private final String _field;

  private String[] _filters;
  private int _rows;
  private String[] _resultFields = {};
  private FormatType _format;

  public SolrTermQueryBuilder(String field) {
    _field = field;
  }

  public SolrTermQueryBuilder values(String... vals) {
    _filters = vals;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder maxRows(int rows) {
    _rows = rows;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder resultFields(String... fields) {
    _resultFields = fields;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder resultFormat(FormatType ft) {
    _format = ft;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder sortBy(String field, SortDirection dir) {
    _sort.put(field, dir);
    return this;
  }

  @SuppressWarnings("unused")
  public String buildQuery() {
    return toString();
  }

  @Override
  public String toString() {
    var query = new StringBuilder();

    filterString(query);
    resultFieldsString(query);
    resultRowsString(query);
    resultFormatString(query);
    resultSortString(query);

    return query.toString();
  }

  private void resultSortString(final StringBuilder sb) {
    if (!_sort.isEmpty()) {
      sb.append('&')
        .append(PARAM_SORT)
        .append('=')
        .append(URLEncoder.encode(
          _sort.entrySet().stream()
            .map(e -> String.format("%s %s",
              e.getKey(), e.getValue().name().toLowerCase()))
            .collect(Collectors.joining(",")),
          StandardCharsets.UTF_8
        ));
    }

  }

  private void resultFormatString(final StringBuilder sb) {
    if (_format != null) {
      sb.append('&')
        .append(PARAM_FORMAT)
        .append('=')
        .append(_format.name().toLowerCase());
    }
  }

  private void resultRowsString(final StringBuilder sb) {
    if (_rows > 0) {
      sb.append('&')
        .append(PARAM_ROWS)
        .append('=')
        .append(_rows);
    }
  }

  private void resultFieldsString(final StringBuilder sb) {
    if (_resultFields.length > 0) {
      sb.append('&')
        .append(PARAM_COLUMNS)
        .append('=')
        .append(URLEncoder.encode(
          String.join(",", List.of(_resultFields)),
          StandardCharsets.UTF_8
        ));
    }
  }

  private void filterString(final StringBuilder sb) {
    sb.append(PARAM_FILTER).append('=');

    var query = new StringBuilder("{!terms f=").append(_field).append('}');

    if (_filters.length == 0) {
      query.append("*");
      return;
    }

    query.append(_filters[0]);

    for (var i = 1; i < _filters.length; i++)
      query.append(',').append(_filters[i]);

    sb.append(URLEncoder.encode(query.toString(), StandardCharsets.UTF_8));
  }
}
