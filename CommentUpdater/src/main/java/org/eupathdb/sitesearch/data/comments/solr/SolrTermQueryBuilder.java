package org.eupathdb.sitesearch.data.comments.solr;

import org.gusdb.fgputil.SortDirection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

  private final Map<String, SortDirection> sort = new LinkedHashMap<>();
  private final String field;

  private String[] filters;
  private int rows;
  private String[] resultFields = {};
  private FormatType format;


  public SolrTermQueryBuilder(String field) {
    this.field = field;
  }

  public SolrTermQueryBuilder values(String... vals) {
    filters = vals;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder maxRows(int rows) {
    this.rows = rows;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder resultFields(String... fields) {
    this.resultFields = fields;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder resultFormat(FormatType ft) {
    this.format = ft;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrTermQueryBuilder sortBy(String field, SortDirection dir) {
    this.sort.put(field, dir);
    return this;
  }

  @SuppressWarnings("unused")
  public String buildQuery() {
    return this.toString();
  }

  @Override
  public String toString() {
    var query = new StringBuilder();

    this.filterString(query);
    this.resultFieldsString(query);
    this.resultRowsString(query);
    this.resultFormatString(query);
    this.resultSortString(query);

    return query.toString();
  }

  private void resultSortString(final StringBuilder sb) {
    if (!sort.isEmpty()) {
      sb.append('&')
        .append(PARAM_SORT)
        .append('=')
        .append(URLEncoder.encode(
          sort.entrySet().stream()
            .map(e -> String.format("%s %s",
              e.getKey(), e.getValue().name().toLowerCase()))
            .collect(Collectors.joining(",")),
          StandardCharsets.UTF_8
        ));
    }

  }

  private void resultFormatString(final StringBuilder sb) {
    if (format != null) {
      sb.append('&')
        .append(PARAM_FORMAT)
        .append('=')
        .append(format.name().toLowerCase());
    }
  }

  private void resultRowsString(final StringBuilder sb) {
    if (rows > 0) {
      sb.append('&')
        .append(PARAM_ROWS)
        .append('=')
        .append(rows);
    }
  }

  private void resultFieldsString(final StringBuilder sb) {
    if (resultFields.length > 0) {
      sb.append('&')
        .append(PARAM_COLUMNS)
        .append('=')
        .append(URLEncoder.encode(
          String.join(",", List.of(this.resultFields)),
          StandardCharsets.UTF_8
        ));
    }
  }

  private void filterString(final StringBuilder sb) {
    sb.append(PARAM_FILTER).append('=');

    var query = new StringBuilder("{!terms f=").append(field).append('}');

    if (filters.length == 0) {
      query.append("*");
      return;
    }

    query.append(filters[0]);

    for (var i = 1; i < filters.length; i++)
      query.append(',').append(filters[i]);

    sb.append(URLEncoder.encode(query.toString(), StandardCharsets.UTF_8));
  }
}
