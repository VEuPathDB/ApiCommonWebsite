package org.apidb.apicommon.service.services.ai;

import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

/**
 * Stage 0 — the synchronous prelude that runs entirely on the request thread
 * (target &lt;2 s, no LLM calls). Validates the request, resolves gene synonyms
 * in-process, computes the content-digest {@code jobId}, then consults the
 * {@code comment_ai_run} cache and the in-memory {@link JobRegistry} to either
 * return a terminal cache-hit, attach the caller as a follower of an in-flight
 * job, or spawn a fresh pipeline.
 */
public class SyncPrelude {

  private final WdkModel _wdkModel;
  private final JobRegistry _registry;

  public SyncPrelude(WdkModel wdkModel, JobRegistry registry) {
    _wdkModel = wdkModel;
    _registry = registry;
  }

  private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

  /**
   * 0a — validate request shape. PubMed requests need a {@code pubmed_id};
   * upload requests need non-empty {@code paper_text} and a 64-char hex
   * {@code pdf_content_sha256}. Throws {@link BadRequestException} (400) on any
   * violation. Static + dependency-free so it is unit-testable in isolation.
   */
  public static void validate(AiGenePublicationRequest request) {
    if (request == null)
      throw new BadRequestException("request body is required");
    if (isBlank(request.geneId))
      throw new BadRequestException("gene_id is required");

    String type = request.documentType == null ? "" : request.documentType.trim();
    switch (type) {
      case "pubmed":
        if (isBlank(request.pubmedId))
          throw new BadRequestException("pubmed_id is required when document_type=pubmed");
        break;
      case "upload":
        if (isBlank(request.paperText))
          throw new BadRequestException("paper_text is required when document_type=upload");
        if (request.pdfContentSha256 == null
            || !SHA256_HEX.matcher(request.pdfContentSha256).matches())
          throw new BadRequestException(
              "pdf_content_sha256 must be a 64-character hex string when document_type=upload");
        break;
      default:
        throw new BadRequestException("document_type must be 'pubmed' or 'upload'");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  /** 0b — resolve gene id + aliases (404 if the stable id is unknown). */
  public List<String> resolveSynonyms(String geneId) throws WdkModelException {
    throw new UnsupportedOperationException("SyncPrelude.resolveSynonyms — deliverable 1");
  }

  /**
   * 0c — compute {@code job_id = sha256(geneId ‖ sortedSynonyms ‖ sourceKey ‖
   * modelName ‖ promptVersion ‖ optionsCanonicalJson)}.
   */
  public String computeJobId(AiGenePublicationRequest request, List<String> sortedSynonyms) {
    throw new UnsupportedOperationException("SyncPrelude.computeJobId — deliverable 1");
  }
}
