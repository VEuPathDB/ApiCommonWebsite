package org.eupathdb.sitesearch.data.comments;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class RecordInfoTest {

  @Test
  void readRs() throws SQLException {
    var sourceId = "source_id";
    var recordType = "record_type";
    var commentId = 31;
    var mockRs = Mockito.mock(ResultSet.class);

    Mockito.when(mockRs.getString(1)).thenReturn(sourceId)
      .thenThrow(new RuntimeException("should not be called twice"));
    Mockito.when(mockRs.getString(2)).thenReturn(recordType)
      .thenThrow(new RuntimeException("should not be called twice"));
    Mockito.when(mockRs.getInt(3)).thenReturn(commentId)
      .thenThrow(new RuntimeException("should not be called twice"));

    var test = new RecordInfo();
    test.readRs(mockRs);

    assertEquals(sourceId, test.sourceId);
    assertEquals(recordType, test.recordType);
    assertEquals(commentId, test.commentId);
  }

  @Test
  void copy() {
    var test = new RecordInfo("apples", "oranges", 86).copy();
    assertEquals(86, test.commentId);
    assertEquals("oranges", test.recordType);
    assertEquals("apples", test.sourceId);
  }

  @Test
  void toSolrId() {
    var test =  new RecordInfo("happy", "birthday", 0);
    assertEquals("birthday__happy", test.toSolrId());
  }
}
