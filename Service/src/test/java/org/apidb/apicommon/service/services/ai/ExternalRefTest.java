package org.apidb.apicommon.service.services.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.ws.rs.BadRequestException;

import org.apidb.apicommon.model.comment.pojo.ExternalRefKind;
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
    ExternalRef.Result r = ExternalRef.normalise("   ", ExternalRefKind.PUBMED);
    assertNull(r.ref);
    assertNull(r.kind);
  }

  @Test
  public void pubmed_plainDigits() {
    ExternalRef.Result r = ExternalRef.normalise("12345678", ExternalRefKind.PUBMED);
    assertEquals("12345678", r.ref);
    assertEquals(ExternalRefKind.PUBMED, r.kind);
  }

  @Test
  public void pubmed_stripsPrefixAndWhitespace() {
    ExternalRef.Result r = ExternalRef.normalise("  PMID: 12345678 ", ExternalRefKind.PUBMED);
    assertEquals("12345678", r.ref);
    assertEquals(ExternalRefKind.PUBMED, r.kind);
  }

  @Test(expected = BadRequestException.class)
  public void pubmed_rejectsNonDigits() {
    ExternalRef.normalise("abc123", ExternalRefKind.PUBMED);
  }

  @Test
  public void doi_plain() {
    ExternalRef.Result r = ExternalRef.normalise("10.1234/abc.def", ExternalRefKind.DOI);
    assertEquals("10.1234/abc.def", r.ref);
    assertEquals(ExternalRefKind.DOI, r.kind);
  }

  @Test
  public void doi_stripsUrlPrefix() {
    ExternalRef.Result r = ExternalRef.normalise("https://doi.org/10.1234/abc.def", ExternalRefKind.DOI);
    assertEquals("10.1234/abc.def", r.ref);
    assertEquals(ExternalRefKind.DOI, r.kind);
  }

  @Test
  public void doi_stripsDxUrlPrefix() {
    ExternalRef.Result r = ExternalRef.normalise("https://dx.doi.org/10.1234/abc.def", ExternalRefKind.DOI);
    assertEquals("10.1234/abc.def", r.ref);
    assertEquals(ExternalRefKind.DOI, r.kind);
  }

  @Test(expected = BadRequestException.class)
  public void doi_rejectsNonDoi() {
    ExternalRef.normalise("not-a-doi", ExternalRefKind.DOI);
  }

  @Test(expected = BadRequestException.class)
  public void refPresent_rejectsMissingKind() {
    ExternalRef.normalise("12345678", null);
  }

  @Test(expected = BadRequestException.class)
  public void refPresent_rejectsUnknownKind() {
    // an unrecognised external_ref_kind parses leniently to null, which a
    // present ref rejects the same way a missing kind does.
    ExternalRef.normalise("12345678", ExternalRefKind.fromWire("isbn"));
  }

  @Test(expected = BadRequestException.class)
  public void kindMismatch_pubmedKindWithDoiValue() {
    ExternalRef.normalise("10.1234/abc", ExternalRefKind.PUBMED);
  }
}
