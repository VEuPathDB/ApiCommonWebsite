package org.apidb.apicommon.service.services.ai;

/**
 * Central constants for the AI gene-publication summary feature that are baked
 * into the {@code jobId} digest (and therefore the {@code comment_ai_run} cache
 * key). Changing either value intentionally invalidates the cache.
 */
public final class AiSummaryConfig {

  /**
   * Claude model used for the summary stages. The prompts were originally tuned
   * against the Python reference pipeline ({@code VPDB_AI_gene_paper_summary}).
   *
   * <p>{@link AnthropicJsonClient} constrains JSON output via structured outputs
   * ({@code output_config.format}) rather than an assistant prefill, since
   * prefill is unsupported on Sonnet 4.6+ / Opus 4.6+ / Fable 5.
   */
  public static final String MODEL_NAME = "claude-sonnet-4-6";

  /**
   * Manually-bumped prompt version folded into the digest. Bump this whenever
   * the {@code getGeneSummary} prompt files change, so edited prompts produce
   * fresh cache entries rather than serving stale output. (The pipeline runs a
   * single LLM prompt stage; the verifyGeneSummary validation pass was dropped
   * on 2026-06-05.)
   */
  public static final String PROMPT_VERSION = "1";

  private AiSummaryConfig() {}
}
