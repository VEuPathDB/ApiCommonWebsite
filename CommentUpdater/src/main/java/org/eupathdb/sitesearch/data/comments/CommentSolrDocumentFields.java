package org.eupathdb.sitesearch.data.comments;

public abstract class CommentSolrDocumentFields {
 
    String getIdFieldName() { return "id"; }
    String getWdkIdFieldName() { return "wdkPrimaryKeyString"; }
    String getDocTypeFieldName() { return "document-type"; }
    String getBatchIdFieldName() { return "batch-id"; }
    String getBatchNameFieldName() { return "batch-name"; }
    String getBatchTypeFieldName() { return "batch-type"; }
    String getBatchTimeFieldName() { return "batch-timestamp"; }
    
    abstract String getCommentIdFieldName();
    abstract String getCommentContentFieldName();

    /**
     * If this is changed in any way, the implementation of
     * {@link #readCsvRow(String)} must also be changed.
     */
    public String[] getRequiredFields() {
      String[] flds = {
        getIdFieldName(),
        getWdkIdFieldName(),
        getDocTypeFieldName(),
        getBatchIdFieldName(),
        getBatchNameFieldName(),
        getBatchTypeFieldName(),
        getBatchTimeFieldName(),
        getCommentIdFieldName()};
      return flds;
    };
}
