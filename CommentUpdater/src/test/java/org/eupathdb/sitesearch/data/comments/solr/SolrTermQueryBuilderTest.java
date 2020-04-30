package org.eupathdb.sitesearch.data.comments.solr;

import org.gusdb.fgputil.SortDirection;
import org.junit.jupiter.api.Test;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class SolrTermQueryBuilderTest {

  @Test
  void values() {
    var test = new SolrTermQueryBuilder("need")
      .values("shout", "gone", "time", "star");
    var exp = "q=" + encode("{!terms f=need}shout,gone,time,star", UTF_8);
    assertEquals(exp, test.toString());
  }

  @Test
  void maxRows() {
    var test = new SolrTermQueryBuilder("control")
      .values("walls", "worlds")
      .maxRows(36);
    var exp = "q=" + encode("{!terms f=control}walls,worlds", UTF_8) + "&rows=36";
    assertEquals(exp, test.toString());
  }

  @Test
  void resultFields() {
    var test = new SolrTermQueryBuilder("control")
      .values("walls", "worlds")
      .resultFields("horse", "guilt");
    var exp = "q=" + encode("{!terms f=control}walls,worlds", UTF_8)
      + "&fl=" + encode("horse,guilt", UTF_8);
    assertEquals(exp, test.toString());
  }

  @Test
  void resultFormat() {
    var test = new SolrTermQueryBuilder("control")
      .values("alone", "feather")
      .resultFormat(FormatType.XML);
    var exp = "q=" + encode("{!terms f=control}alone,feather", UTF_8) + "&wt=xml";
    assertEquals(exp, test.toString());
  }

  @Test
  void sortBy() {
    var test = new SolrTermQueryBuilder("control")
      .values("Ratt", "sounds")
      .sortBy("cloud", SortDirection.ASC)
      .sortBy("storm", SortDirection.DESC);
    var exp = "q=" + encode("{!terms f=control}Ratt,sounds", UTF_8)
      + "&sort=" + encode("cloud asc,storm desc", UTF_8);
    assertEquals(exp, test.toString());
  }

  @Test
  void buildQuery() {
    var test = new SolrTermQueryBuilder("control")
      .values("oath", "arrows")
      .resultFields("scent", "Oklahoma")
      .resultFormat(FormatType.CSV)
      .maxRows(89)
      .sortBy("safety", SortDirection.ASC)
      .sortBy("trepidation", SortDirection.DESC);
    var exp = "q=" + encode("{!terms f=control}oath,arrows", UTF_8)
      + "&fl=" + encode("scent,Oklahoma", UTF_8)
      + "&rows=89"
      + "&wt=csv"
      + "&sort=" + encode("safety asc,trepidation desc", UTF_8);
    assertEquals(exp, test.buildQuery());
    assertEquals(exp, test.toString());
  }
}
