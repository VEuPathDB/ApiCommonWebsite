package org.eupathdb.sitesearch.data.comments.solr;

public enum Op {
  AND,
  OR;

  @Override
  public String toString() {
    return " " + name() + " ";
  }
}
