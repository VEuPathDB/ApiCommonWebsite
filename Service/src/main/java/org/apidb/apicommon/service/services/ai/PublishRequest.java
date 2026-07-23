package org.apidb.apicommon.service.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The POST body for {@code /user-comments/ai-gene-publication/{job_id}/publish}.
 * Carries only the user's (possibly edited) reviewed text — the back end reads
 * the gene target and AI provenance from the cached {@code comment_ai_run} row
 * keyed by the path's {@code job_id} and derives {@code is_edited} by comparing
 * this text to the run's AI original (so the front end neither sends nor picks it).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishRequest {

  public String headline;
  public String content;
}
