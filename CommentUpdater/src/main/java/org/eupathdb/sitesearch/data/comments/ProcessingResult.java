package org.eupathdb.sitesearch.data.comments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class ProcessingResult {
  private final static String NL = System.lineSeparator();  

  
  List<SolrDocument> toUpdate    = new ArrayList<>();
  List<RecordInfo> toPostProcess = new ArrayList<>();
  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Solr Documents:" + NL);
    buf.append(toUpdate.stream().map(SolrDocument::toString)
        .collect(Collectors.joining("NL")) + NL);
    buf.append("Records:" + NL);
    buf.append(toPostProcess.stream().map(RecordInfo::toString)
        .collect(Collectors.joining("NL")) + NL);
    return buf.toString();
  }
}

