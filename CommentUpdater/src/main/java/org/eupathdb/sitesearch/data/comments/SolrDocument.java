package org.eupathdb.sitesearch.data.comments;

import java.util.Arrays;


class SolrDocument {

  private final static String NL = System.lineSeparator();  
  
  private String    solrId;
  private String    sourceId;
  private String    documentType;
  private String    batchId;
  private String    batchName;
  private String    batchType;
  private long      batchTime;
  private String[]     commentIds;
  private boolean[] hits;

  String getSourceId() {
    return sourceId;
  }

  String getBatchName() {
    return batchName;
  }

  String getSolrId() {
    return solrId;
  }

  String getDocumentType() {
    return documentType;
  }

  String getBatchId() {
    return batchId;
  }

  String getBatchType() {
    return batchType;
  }

  String getRecordType() {
    return solrId.substring(0, solrId.indexOf("__"));
  }

  long getBatchTime() {
    return batchTime;
  }

  boolean hasCommentId(String commentId) {
    for (var i = 0; i < commentIds.length; i++)
      if (commentIds[i].equals(commentId)) {
        hits[i] = true;
        return true;
      }
    return false;
  }

  boolean hasUnhitComments() {
    for (var b : hits) {
      if (!b)
        return true;
    }
    return false;
  }

  /**
   * Parses a CSV row into a DocumentInfo instance.
   *
   * Expected CSV format:
   * <code>
   *   id,wdkPrimaryKeyString,document-type,batch-id,batch-name,batch-type,batch-timestamp,userCommentIds
   *   gene__PKNH_0107500,PKNH_0107500,gene,organism_pknoH_1582809387,pknoH,organism,1582809387,79450
   *   gene__PKNH_0107600,PKNH_0107600,gene,organism_pknoH_1582809387,pknoH,organism,1582809387,100191083
   *   gene__PKNH_0108200,PKNH_0108200,gene,organism_pknoH_1582809387,pknoH,organism,1582809387,"21283,33830"
   * </code>
   *
   * @param row Individual CSV row to process
   *
   * @return Parsed DocumentInfo object
   *
   * @throws NumberFormatException if the batch-timestamp field is not a valid
   *         long value.
   * @throws NumberFormatException if any of the comment id values are not
   *         valid integers.
   * @throws RuntimeException if the csv row was found to be shorter than the
   *         expected row length.
   */
  static SolrDocument readCsvRow(final String row) {
    var buf = new StringBuilder();
    var len = row.length();
    var field = 1;
    var out = new SolrDocument();

    // Pre-stretch buffer
    buf.setLength(128);
    buf.setLength(0);

    char c;
    for (var i = 0; i < len; i++) {
      c = row.charAt(i);
      if (c == ',') {
        switch (field) {
          case 1: out.solrId       = trimQuotes(buf.toString()); break;
          case 2: out.sourceId     = trimQuotes(buf.toString()); break;
          case 3: out.documentType = trimQuotes(buf.toString()); break;
          case 4: out.batchId      = trimQuotes(buf.toString()); break;
          case 5: out.batchName    = trimQuotes(buf.toString()); break;
          case 6: out.batchType    = trimQuotes(buf.toString()); break;
          case 7:
            out.batchTime = Long.parseLong(buf.toString());
            // fallthrough
          case 8:
            out.commentIds = splitCommentIds(trimQuotes(row.substring(i+1)));
            out.hits       = new boolean[out.commentIds.length];
            i = len;
        }
        buf.setLength(0);
        field++;
      } else {
        buf.append(c);
      }
    }

    // Short csv row?
    if (field < 8)
      throw new RuntimeException("Invalid document CSV row: " + row);

    return out;
  }

  private static String[] splitCommentIds(String in) {
    if (in.isEmpty())
      return new String[0];

    var tmp = in.split(",");
    var out = new String[tmp.length];

    for (var i = 0; i < tmp.length; i++)
      out[i] = tmp[i];
    Arrays.sort(out);
    return out;
  }

  private static String trimQuotes(final String in) {
    if (in.isEmpty())
      return in;

    if (in.charAt(0) == '"') {
      return in.substring(1, in.length() - 1);
    }
    return in;
  }
  
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Solr ID: " + solrId + NL);
    buf.append("Source ID: " + sourceId + NL);
    buf.append("Comment IDs: " + String.join(",", commentIds) + NL);    
    return buf.toString();
  }


}
