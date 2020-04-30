package org.eupathdb.sitesearch.data.comments.solr;

import org.gusdb.fgputil.SortDirection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper for building Solr queries.
 */
public class SolrUrlQueryBuilder {
  private static final String
    PARAM_FILTER  = "q",
    PARAM_COLUMNS = "fl",
    PARAM_SORT    = "sort",
    PARAM_ROWS    = "rows",
    PARAM_FORMAT  = "wt";

  final private List<Filter> filters = new ArrayList<>();
  final private Map<String, SortDirection> sort = new LinkedHashMap<>();
  final private String url;

  private int rows;
  private String[] resultFields = {};
  private FormatType format;

  private SolrUrlQueryBuilder(final String url) {
    this.url = url;
  }

  public static SolrUrlQueryBuilder select(String url) {
    return new SolrUrlQueryBuilder(url + (url.endsWith("/") ? "select" : "/select"));
  }

  public String getUrl() {
    return this.url;
  }

  /**
   * Appends a case with the given operator to the filter
   * with 0 or more filter strings each joined with the
   * provided secondary operator.
   *
   * Multi filter output
   * <code>
   *   ... {join: AND|OR} {field}:({filter[0]} {op: AND|OR} {filter[1]} {op: AND|OR} ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... {join: AND|OR} {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... {join: AND|OR} {field}:*
   * </code>
   *
   * @param join    Operator to use when joining this case
   *                to the overall query
   * @param op      Operator to use when applying filters to
   *                the given field.
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  public SolrUrlQueryBuilder filter(Op join, Op op, String field, String... filters) {
    this.filters.add(new Filter(join, op, field, filters));
    return this;
  }

  /**
   * Appends an AND case to the filter with 0 or more filter
   * strings each joined with the provided operator.
   *
   * Multi filter output
   * <code>
   *   ... AND {field}:({filter[0]} {AND|OR} {filter[1]} {AND|OR} ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... AND {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... AND {field}:*
   * </code>
   *
   * @param op      Operator to use when applying filters to
   *                the given field.
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterAnd(Op op, String field, String... filters) {
    return filter(Op.AND, op, field, filters);
  }

  /**
   * Appends an AND case to the filter with 0 or more filter
   * strings each joined with OR.
   *
   * Multi filter output
   * <code>
   *   ... AND {field}:({filter[0]} OR {filter[1]} OR ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... AND {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... AND {field}:*
   * </code>
   *
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterAndAnyOf(String field, String... filters) {
    return filter(Op.AND, Op.OR, field, filters);
  }

  /**
   * Appends an AND case to the filter with 0 or more filter
   * strings each joined with AND.
   *
   * Multi filter output
   * <code>
   *   ... AND {field}:({filter[0]} AND {filter[1]} AND ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... AND {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... AND {field}:*
   * </code>
   *
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterAndAllOf(String field, String... filters) {
    return filter(Op.AND, Op.AND, field, filters);
  }

  /**
   * Appends an OR case to the filter with 0 or more filter
   * strings each joined with the provided operator.
   *
   * Multi filter output
   * <code>
   *   ... OR {field}:({filter[0]} {AND|OR} {filter[1]} {AND|OR} ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... OR {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... OR {field}:*
   * </code>
   *
   * @param op      Operator to use when applying filters to
   *                the given field.
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterOr(Op op, String field, String... filters) {
    return filter(Op.OR, op, field, filters);
  }

  /**
   * Appends an OR case to the filter with 0 or more filter
   * strings each joined with OR.
   *
   * Multi filter output
   * <code>
   *   ... OR {field}:({filter[0]} OR {filter[1]} OR ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... OR {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... OR {field}:*
   * </code>
   *
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterOrAnyOf(String field, String... filters) {
    return filter(Op.OR, Op.OR, field, filters);
  }

  /**
   * Appends an OR case to the filter with 0 or more filter
   * strings each joined with AND.
   *
   * Multi filter output
   * <code>
   *   ... OR {field}:({filter[0]} AND {filter[1]} AND ...)
   * </code>
   *
   * Single filter output
   * <code>
   *   ... OR {field}:{filter[0]}
   * </code>
   *
   * Zero filter output
   * <code>
   *   ... OR {field}:*
   * </code>
   *
   * @param field   Field on which to filter
   * @param filters Filter text
   *
   * @return this builder
   */
  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder filterOrAllOf(String field, String... filters) {
    return filter(Op.OR, Op.AND, field, filters);
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder maxRows(int rows) {
    this.rows = rows;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder resultFields(String... fields) {
    this.resultFields = fields;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder resultFormat(FormatType ft) {
    this.format = ft;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder sortBy(String field, SortDirection dir) {
    this.sort.put(field, dir);
    return this;
  }

  @SuppressWarnings("unused")
  public String buildQuery() {
    return this.toString();
  }

  /**
   * Returns only the query string as a url encoded text blob.
   *
   * Useful for making POST select queries to Solr.
   */
  public String queryString() {
    var query = new StringBuilder();

    this.filterString(query);
    this.resultFieldsString(query);
    this.resultRowsString(query);
    this.resultFormatString(query);
    this.resultSortString(query);

    return query.toString();
  }

  @Override
  public String toString() {
    var query = new StringBuilder(this.url).append('?');

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

    var query = new StringBuilder();

    if (filters.isEmpty()) {
      query.append("*:*");
      return;
    }

    var len = filters.size();
    query.append(filters.get(0).toString());

    for (var i = 1; i < len; i++)
      query.append(filters.get(i).topOp).append(filters.get(i));

    sb.append(URLEncoder.encode(query.toString(), StandardCharsets.UTF_8));
  }

  private static class Filter {
    final Op       topOp;
    final Op       op;
    final String   field;
    final String[] filters;

    Filter(Op topOp, Op op, String field, String[] filters) {
      this.topOp = topOp;
      this.op = op;
      this.field = field;
      this.filters = filters;
    }

    @Override
    public String toString() {
      if (filters.length == 0)
        return field + ":*";
      if (filters.length == 1)
        return field + ":\"" + filters[0] + "\"";

      var out = new StringBuilder(field).append(":(\"").append(filters[0]).append('"');

      for (var i = 1; i < filters.length; i++)
        out.append(op).append('"').append(filters[i]).append('"');

      return out.append(')').toString();
    }
  }
}
