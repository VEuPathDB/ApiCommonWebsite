package org.apidb.apicommon.service.services.ai;

import javax.ws.rs.BadRequestException;

import org.apidb.apicommon.model.comment.pojo.ExternalRefKind;
import org.apidb.apicommon.model.comment.pojo.SourceKind;
import org.junit.Test;

public class SyncPreludeTest {

  private static AiGenePublicationRequest pubmedRequest() {
    AiGenePublicationRequest r = new AiGenePublicationRequest();
    r.geneId = "PF3D7_1133400";
    r.documentType = SourceKind.PUBMED;
    r.pubmedId = "12345678";
    return r;
  }

  private static AiGenePublicationRequest uploadRequest() {
    AiGenePublicationRequest r = new AiGenePublicationRequest();
    r.geneId = "PF3D7_1133400";
    r.documentType = SourceKind.UPLOAD;
    r.paperText = "The gene PF3D7_1133400 is discussed at length.";
    r.pdfContentSha256 = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"; // 64 hex
    return r;
  }

  @Test
  public void validate_acceptsValidPubmedRequest() {
    SyncPrelude.validate(pubmedRequest());
  }

  @Test
  public void validate_acceptsValidUploadRequest() {
    SyncPrelude.validate(uploadRequest());
  }

  @Test(expected = BadRequestException.class)
  public void validate_rejectsMissingGeneId() {
    AiGenePublicationRequest r = pubmedRequest();
    r.geneId = "  ";
    SyncPrelude.validate(r);
  }

  @Test(expected = BadRequestException.class)
  public void validate_rejectsUnknownDocumentType() {
    AiGenePublicationRequest r = pubmedRequest();
    r.documentType = SourceKind.fromWire("carrier-pigeon"); // unrecognised → null
    SyncPrelude.validate(r);
  }

  @Test(expected = BadRequestException.class)
  public void validate_rejectsPubmedWithoutPubmedId() {
    AiGenePublicationRequest r = pubmedRequest();
    r.pubmedId = null;
    SyncPrelude.validate(r);
  }

  @Test(expected = BadRequestException.class)
  public void validate_rejectsUploadWithoutPaperText() {
    AiGenePublicationRequest r = uploadRequest();
    r.paperText = "";
    SyncPrelude.validate(r);
  }

  @Test(expected = BadRequestException.class)
  public void validate_rejectsUploadWithMalformedSha() {
    AiGenePublicationRequest r = uploadRequest();
    r.pdfContentSha256 = "not-a-valid-sha";
    SyncPrelude.validate(r);
  }

  @Test
  public void validate_normalisesUploadPubmedRef() {
    AiGenePublicationRequest r = uploadRequest();
    r.externalRef = "  PMID: 12345678 ";
    r.externalRefKind = ExternalRefKind.PUBMED;
    SyncPrelude.validate(r);
    org.junit.Assert.assertEquals("12345678", r.externalRef);
    org.junit.Assert.assertEquals(ExternalRefKind.PUBMED, r.externalRefKind);
  }

  @Test
  public void validate_normalisesUploadDoiRef() {
    AiGenePublicationRequest r = uploadRequest();
    r.externalRef = "https://doi.org/10.1234/abc.def";
    r.externalRefKind = ExternalRefKind.DOI;
    SyncPrelude.validate(r);
    org.junit.Assert.assertEquals("10.1234/abc.def", r.externalRef);
    org.junit.Assert.assertEquals(ExternalRefKind.DOI, r.externalRefKind);
  }

  @Test(expected = javax.ws.rs.BadRequestException.class)
  public void validate_rejectsMalformedExternalRef() {
    AiGenePublicationRequest r = uploadRequest();
    r.externalRef = "not-a-pmid";
    r.externalRefKind = ExternalRefKind.PUBMED;
    SyncPrelude.validate(r);
  }

  @Test
  public void validate_clearsExternalRefOnPubmedPath() {
    AiGenePublicationRequest r = pubmedRequest();
    r.externalRef = "12345678";
    r.externalRefKind = ExternalRefKind.PUBMED;
    SyncPrelude.validate(r);
    org.junit.Assert.assertNull(r.externalRef);
    org.junit.Assert.assertNull(r.externalRefKind);
  }

  @Test
  public void validate_acceptsUploadWithNoExternalRef() {
    AiGenePublicationRequest r = uploadRequest(); // no external_ref set
    SyncPrelude.validate(r);
    org.junit.Assert.assertNull(r.externalRef);
    org.junit.Assert.assertNull(r.externalRefKind);
  }
}
