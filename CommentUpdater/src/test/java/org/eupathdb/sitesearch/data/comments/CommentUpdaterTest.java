package org.eupathdb.sitesearch.data.comments;

import org.eupathdb.sitesearch.data.comments.CommentUpdater.DocumentCommentsInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentUpdaterTest {

  @Test
  void buildUpdateJson() {
    var doc = DocumentInfo.readCsvRow(DocumentInfoTest.VALID_CSV_ROW);
    var com = new DocumentCommentsInfo();
    com.commentIds.add(1);
    com.commentContents.add("first comment");
    com.commentIds.add(2);
    com.commentContents.add("second comment");

    var tArr = CommentUpdater.buildUpdateJson(doc, com);

    assertEquals(1, tArr.length());
    assertTrue(tArr.get(0) instanceof JSONObject);

    var tObj = tArr.getJSONObject(0);
    assertEquals(DocumentInfoTest.VALID_SOLR_ID, tObj.getString(Field.ID));
    assertEquals(DocumentInfoTest.VALID_DOC_TYPE, tObj.getString(Field.DOC_TYPE));
    assertEquals(DocumentInfoTest.VALID_BATCH_ID, tObj.getString(Field.BATCH_ID));
    assertEquals(DocumentInfoTest.VALID_BATCH_NAME, tObj.getString(Field.BATCH_NAME));
    assertEquals(DocumentInfoTest.VALID_BATCH_TYPE, tObj.getString(Field.BATCH_TYPE));
    assertEquals(DocumentInfoTest.VALID_BATCH_TIME, tObj.getLong(Field.BATCH_TIME));

    assertTrue(tObj.get(Field.COMMENT_ID) instanceof JSONObject);
    assertTrue(tObj.get(Field.COMMENT_TXT) instanceof JSONObject);

    var tComIdObj = tObj.getJSONObject(Field.COMMENT_ID);
    assertTrue(tComIdObj.has("set"));
    assertTrue(tComIdObj.get("set") instanceof JSONArray);

    var tComIds = tComIdObj.getJSONArray("set");
    assertEquals(2, tComIds.length());
    assertEquals(tComIds.get(0), 1);
    assertEquals(tComIds.get(1), 2);

    var tComTxtObj = tObj.getJSONObject(Field.COMMENT_TXT);
    assertTrue(tComTxtObj.has("set"));
    assertTrue(tComTxtObj.get("set") instanceof JSONArray);

    var tComTxt = tComTxtObj.getJSONArray("set");
    assertEquals(2, tComTxt.length());
    assertEquals(tComTxt.get(0), "first comment");
    assertEquals(tComTxt.get(1), "second comment");
  }

  @Test
  void buildCommentLookupQuery() {
    assertNotNull(CommentUpdater.buildCommentLookupQuery("foo"));
  }

  @Test
  void buildFindUpdatableCommentsSql() {
    assertNotNull(CommentUpdater.buildFindUpdatableCommentsSql("some_schema"));
  }

  @Test
  void buildUpdateSingleSql() {
    assertNotNull(CommentUpdater.buildUpdateSingleSql("a_schema"));
  }

  @Test
  void buildDocumentByIdSolr() {
    var test = CommentUpdater
      .buildDocumentByIdSolr(new String[]{"apple", "banana"});
    assertNotNull(test);
    assertTrue(test.contains("apple"));
    assertTrue(test.contains("banana"));
  }

  @Test
  void handleResult() throws SQLException {
    var tests = new Object[][]{
      {64, "happy"},
      {86, "happy"},
      {0, ""}
    };

    var mock = mock(ResultSet.class);
    when(mock.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(mock.getInt("comment_id"))
      .thenReturn((Integer) tests[0][0])
      .thenReturn((Integer) tests[1][0])
      .thenReturn((Integer) tests[2][0]);
    when(mock.getString("content"))
      .thenReturn((String) tests[0][1])
      .thenReturn((String) tests[1][1])
      .thenReturn((String) tests[2][1]);

    var test = CommentUpdater.handleResult(mock);

    for (int i = 0; i < tests.length - 1; i++) {
      assertEquals(test.commentIds.get(i), tests[i][0]);
      assertEquals(test.commentContents.get(i), tests[i][1]);
    }
  }

  @Test
  void buildCommentedRecordsSolr() {
    assertNotNull(CommentUpdater.buildCommentedRecordsSolr("someUrl"));
  }

  @Test
  void handleSingle() throws SQLException {
    var mock = mock(ResultSet.class);
    when(mock.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(mock.getString(1)).thenReturn("foo").thenReturn("bar");

    assertArrayEquals(CommentUpdater.handleSingle(mock),
      new String[]{"gene__foo", "gene__bar"});
  }

  @Test
  void entityReader() {
    var mock1 = mock(InputStream.class);
    var mock2 = mock(Response.class);
    when(mock2.getEntity()).thenReturn(mock1);
    assertNotNull(CommentUpdater.entityReader(mock2));
  }
}

