package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.ws.rs.BadRequestException;

import org.junit.Test;

public class ExternalRefTest {

  @Test
  public void bothBlank_yieldsNullNull() {
    ExternalRef.Result r = ExternalRef.normalise(null, null);
    assertNull(r.ref);
    assertNull(r.kind);
  }

  @Test
  public void blankRef_ignoresKind() {
    ExternalRef.Result r = ExternalRef.normalise("   ", "pubmed");
    assertNull(r.ref);
    assertNull(r.kind);
  }

  @Test
  public void pubmed_plainDigits() {
    ExternalRef.Result r = ExternalRef.normalise("12345678", "pubmed");
    assertEquals("12345678", r.ref);
    assertEquals("pubmed", r.kind);
  }

  @Test
  public void pubmed_stripsPrefixAndWhitespace() {
    ExternalRef.Result r = ExternalRef.normalise("  PMID: 12345678 ", "pubmed");
    assertEquals("12345678", r.ref);
    assertEquals("pubmed", r.kind);
  }

  @Test(expected = BadRequestException.class)
  public void pubmed_rejectsNonDigits() {
    ExternalRef.normalise("abc123", "pubmed");
  }

  @Test
  public void doi_plain() {
    ExternalRef.Result r = ExternalRef.normalise("10.1234/abc.def", "doi");
    assertEquals("10.1234/abc.def", r.ref);
    assertEquals("doi", r.kind);
  }

  @Test
  public void doi_stripsUrlPrefix() {
    ExternalRef.Result r = ExternalRef.normalise("https://doi.org/10.1234/abc.def", "doi");
    assertEquals("10.1234/abc.def", r.ref);
    assertEquals("doi", r.kind);
  }

  @Test(expected = BadRequestException.class)
  public void doi_rejectsNonDoi() {
    ExternalRef.normalise("not-a-doi", "doi");
  }

  @Test(expected = BadRequestException.class)
  public void refPresent_rejectsMissingKind() {
    ExternalRef.normalise("12345678", null);
  }

  @Test(expected = BadRequestException.class)
  public void refPresent_rejectsUnknownKind() {
    ExternalRef.normalise("12345678", "isbn");
  }

  @Test(expected = BadRequestException.class)
  public void kindMismatch_pubmedKindWithDoiValue() {
    ExternalRef.normalise("10.1234/abc", "pubmed");
  }
}
