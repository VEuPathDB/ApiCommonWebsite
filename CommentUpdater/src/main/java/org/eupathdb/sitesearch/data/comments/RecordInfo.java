package org.eupathdb.sitesearch.data.comments;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A tuple holding a record ID from the database (eg a Gene ID)
 *
 * @author Steve
 */
class RecordInfo {

  String recordType;

  String sourceId;

  String commentId;

  RecordInfo(String sourceId, String recordType, String commentId) {
    this.sourceId = sourceId;
    this.recordType = recordType;
    this.commentId = commentId;
  }

  RecordInfo() {}

  void readRs(ResultSet rs) throws SQLException {
    this.sourceId   = rs.getString(1);
    this.recordType = rs.getString(2);
    this.commentId = rs.getObject(3).toString();
  }

  RecordInfo copy() {
    return new RecordInfo(sourceId, recordType, commentId);
  }

  String toSolrId() {
    return recordType + "__" + sourceId;
  }
}
