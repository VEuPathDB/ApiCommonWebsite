package org.apidb.apicommon.service.services.ai;

import java.util.List;

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

  /** 0a — validate request shape (upload requires paper_text + 64-char hex sha). */
  public void validate(AiGenePublicationRequest request) {
    throw new UnsupportedOperationException("SyncPrelude.validate — deliverable 1");
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
