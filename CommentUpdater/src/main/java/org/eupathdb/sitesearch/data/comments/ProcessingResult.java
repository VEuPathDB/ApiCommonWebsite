package org.eupathdb.sitesearch.data.comments;

import java.util.ArrayList;
import java.util.List;

class ProcessingResult {
  List<SolrDocument> toUpdate    = new ArrayList<>();
  List<RecordInfo> toPostProcess = new ArrayList<>();
}

