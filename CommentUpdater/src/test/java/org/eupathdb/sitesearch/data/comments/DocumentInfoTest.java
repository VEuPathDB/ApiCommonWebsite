package org.eupathdb.sitesearch.data.comments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DocumentInfoTest {

  static final String
    // id,wdkPrimaryKeyString,document-type,batch-id,batch-name,batch-type,batch-timestamp,userCommentIds
    VALID_CSV_ROW = "gene__PKNH_0107500," // Solr ID
      + "PKNH_0107500,"                   // WDK Primary Key (Source ID)
      + "gene,"                           // Document Type
      + "organism_pknoH_1582809387,"      // Batch ID
      + "pknoH,"                          // Batch Name
      + "organism,"                       // Batch Type
      + "1582809387,"                     // Batch TimeStamp
      + "321321,123456",                  // User Comment IDs
    VALID_SOLR_ID = "gene__PKNH_0107500",
    VALID_SOURCE_ID = "PKNH_0107500",
    VALID_DOC_TYPE = "gene",
    VALID_BATCH_ID = "organism_pknoH_1582809387",
    VALID_BATCH_NAME = "pknoH",
    VALID_BATCH_TYPE = "organism";

  static final long
    VALID_BATCH_TIME = 1582809387L;
  static final int
    VALID_COMMENT_1 = 321321,
    VALID_COMMENT_2 = 123456;

  @Nested
  class ReadCsvRow {
    @Test
    void withValidCsvRow() {
      final var ref = new AtomicReference<DocumentInfo>();
      assertDoesNotThrow(() -> ref.set(DocumentInfo.readCsvRow(VALID_CSV_ROW)));

      final var test = ref.get();
      assertEquals(VALID_SOLR_ID, test.getSolrId());
      assertEquals(VALID_SOURCE_ID, test.getSourceId());
      assertEquals(VALID_DOC_TYPE, test.getDocumentType());
      assertEquals(VALID_BATCH_ID, test.getBatchId());
      assertEquals(VALID_BATCH_NAME, test.getBatchName());
      assertEquals(VALID_BATCH_TYPE, test.getBatchType());
      assertEquals(VALID_BATCH_TIME, test.getBatchTime());
      assertTrue(test.hasUnhitComments());
      assertTrue(test.hasCommentId(VALID_COMMENT_1));
      assertTrue(test.hasCommentId(VALID_COMMENT_2));
      assertFalse(test.hasUnhitComments());
      assertFalse(test.hasCommentId(654321));
    }
  }
}
