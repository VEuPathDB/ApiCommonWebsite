package org.eupathdb.sitesearch.data.comments.solr;

import org.gusdb.fgputil.SortDirection;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class SolrUrlQueryBuilderTest {

  @Test
  void select() {
    var test1 = SolrUrlQueryBuilder.select("hello");
    assertEquals("hello/select?q=", test1.toString());

    var test2 = SolrUrlQueryBuilder.select("goodbye/");
    assertEquals("goodbye/select?q=", test2.toString());
  }

  @Test
  void getUrl() {
    var test = SolrUrlQueryBuilder.select("happy");
    assertEquals("happy/select", test.getUrl());
  }

  @Test
  void filter() {
    var test1 = SolrUrlQueryBuilder.select("sad").
      filter(Op.AND, Op.OR, "foo", "a", "b", "c");
    var exp1 = "sad/select?q=" + encode("foo:(\"a\" OR \"b\" OR \"c\")", UTF_8);
    assertEquals(exp1, test1.toString());

    var test2 = SolrUrlQueryBuilder.select("sad")
      .filter(Op.AND, Op.OR, "foo", "a", "b", "c")
      .filter(Op.AND, Op.AND, "bar", "d", "e");
    var exp2 = "sad/select?q=" + encode("foo:(\"a\" OR \"b\" OR \"c\") AND bar:(\"d\" AND \"e\")", UTF_8);
    assertEquals(exp2, test2.toString());

    var test3 = SolrUrlQueryBuilder.select("sad")
      .filter(Op.AND, Op.OR, "foo", "a", "b", "c")
      .filter(Op.AND, Op.AND, "bar", "d")
      .filter(Op.OR, Op.OR, "fizz");
    var exp3 = "sad/select?q=" + encode("foo:(\"a\" OR \"b\" OR \"c\") AND bar:\"d\" OR fizz:*", UTF_8);
    assertEquals(exp3, test3.toString());
  }

  @Test
  void filterAnd() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterAnd(Op.AND, "bar", "b", "c").
      filterAnd(Op.OR, "fizz", "d", "e");
    var exp1 = "down/select?q=" + encode("foo:\"a\" AND bar:(\"b\" AND \"c\") AND fizz:(\"d\" OR \"e\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void filterAndAnyOf() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterAndAnyOf("bar", "b", "c");
    var exp1 = "down/select?q=" + encode("foo:\"a\" AND bar:(\"b\" OR \"c\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void filterAndAllOf() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterAndAllOf("bar", "b", "c");
    var exp1 = "down/select?q=" + encode("foo:\"a\" AND bar:(\"b\" AND \"c\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void filterOr() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterOr(Op.AND, "bar", "b", "c").
      filterOr(Op.OR, "fizz", "d", "e");
    var exp1 = "down/select?q=" + encode("foo:\"a\" OR bar:(\"b\" AND \"c\") OR fizz:(\"d\" OR \"e\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void filterOrAnyOf() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterOrAnyOf("bar", "b", "c");
    var exp1 = "down/select?q=" + encode("foo:\"a\" OR bar:(\"b\" OR \"c\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void filterOrAllOf() {
    var test1 = SolrUrlQueryBuilder.select("down").
      filter(Op.AND, Op.OR, "foo", "a").
      filterOrAllOf("bar", "b", "c");
    var exp1 = "down/select?q=" + encode("foo:\"a\" OR bar:(\"b\" AND \"c\")", UTF_8);
    assertEquals(exp1, test1.toString());
  }

  @Test
  void maxRows() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.OR, Op.OR, "foo")
      .maxRows(86);
    assertEquals("up/select?q=" + encode("foo:*", UTF_8) + "&rows=86", test.toString());
  }

  @Test
  void resultFields() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.OR, Op.OR, "foo")
      .resultFields("a", "b", "c");
    var exp = "up/select?q=" + encode("foo:*", UTF_8)
      + "&fl=" + encode("a,b,c", UTF_8);
    assertEquals(exp, test.toString());
  }

  @Test
  void resultFormat() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.OR, Op.OR, "foo")
      .resultFormat(FormatType.CSV);
    var exp = "up/select?q=" + encode("foo:*", UTF_8) + "&wt=csv";
    assertEquals(exp, test.toString());
  }

  @Test
  void sortBy() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.OR, Op.OR, "foo")
      .sortBy("foo", SortDirection.ASC)
      .sortBy("bar", SortDirection.DESC);
    var exp = "up/select?q=" + encode("foo:*", UTF_8)
      + "&sort=" + encode("foo asc,bar desc", UTF_8);
    assertEquals(exp, test.toString());
  }

  @Test
  void buildQuery() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.AND, Op.OR, "foo", "a", "b", "c")
      .filter(Op.AND, Op.AND, "bar", "d")
      .filter(Op.OR, Op.OR, "fizz")
      .resultFormat(FormatType.CSV)
      .resultFields("a", "b", "c")
      .maxRows(86)
      .sortBy("foo", SortDirection.ASC)
      .sortBy("bar", SortDirection.DESC);
    assertEquals(test.toString(), test.buildQuery());
  }

  @Test
  void queryString() {
    var test = SolrUrlQueryBuilder.select("up")
      .filter(Op.AND, Op.OR, "foo", "a", "b", "c")
      .filter(Op.AND, Op.AND, "bar", "d")
      .filter(Op.OR, Op.OR, "fizz")
      .resultFormat(FormatType.JSON)
      .resultFields("a", "b", "c")
      .maxRows(86)
      .sortBy("foo", SortDirection.ASC)
      .sortBy("bar", SortDirection.DESC);
    var exp = "q=" + encode("foo:(\"a\" OR \"b\" OR \"c\") AND bar:\"d\" OR fizz:*", UTF_8)
      + "&fl=" + encode("a,b,c", UTF_8)
      + "&rows=86"
      + "&wt=json"
      + "&sort=" + encode("foo asc,bar desc", UTF_8);
    //
    assertEquals(exp, test.queryString());
  }
}
