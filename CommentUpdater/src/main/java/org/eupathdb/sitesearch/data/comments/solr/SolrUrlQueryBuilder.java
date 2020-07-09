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

  final private List<Filter> _filters = new ArrayList<>();
  final private Map<String, SortDirection> _sort = new HashMap<>();
  final private String _url;

  private int _rows;
  private String[] _resultFields = {};
  private FormatType _format;

  private SolrUrlQueryBuilder(final String url) {
    _url = url;
  }

  public static SolrUrlQueryBuilder select(String url) {
    return new SolrUrlQueryBuilder(url + (url.endsWith("/") ? "select" : "/select"));
  }

  public String getUrl() {
    return _url;
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
    _filters.add(new Filter(join, op, field, filters));
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
    _rows = rows;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder resultFields(String... fields) {
    _resultFields = fields;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder resultFormat(FormatType ft) {
    _format = ft;
    return this;
  }

  @SuppressWarnings("unused")
  public SolrUrlQueryBuilder sortBy(String field, SortDirection dir) {
    _sort.put(field, dir);
    return this;
  }

  @SuppressWarnings("unused")
  public String buildQuery() {
    return toString();
  }

  /**
   * Returns only the query string as a url encoded text blob.
   *
   * Useful for making POST select queries to Solr.
   */
  public String queryString() {
    var query = new StringBuilder();

    filterString(query);
    resultFieldsString(query);
    resultRowsString(query);
    resultFormatString(query);
    resultSortString(query);

    return query.toString();
  }

  @Override
  public String toString() {
    var query = new StringBuilder(_url).append('?');

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

    var query = new StringBuilder();

    if (_filters.isEmpty()) {
      query.append("*:*");
      return;
    }

    var len = _filters.size();
    query.append(_filters.get(0).toString());

    for (var i = 1; i < len; i++)
      query.append(_filters.get(i).topOp).append(_filters.get(i));

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

      var out = new StringBuilder(field).append(":(").append(filters[0]);

      for (var i = 1; i < filters.length; i++)
        out.append(op).append('"').append(filters[i]).append('"');

      return out.append(')').toString();
    }
  }
}
