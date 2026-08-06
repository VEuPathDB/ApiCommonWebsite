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
   * any prompt file changes, so edited prompts produce fresh cache entries rather
   * than serving stale output. Now covers both LLM stages — {@code getGeneSummary}
   * and the compulsory {@code generatePDs} product-description stage.
   *
   * <p>Bumped {@code "1" → "2"} when product descriptions became compulsory (a new
   * {@code generatePDs} stage plus cosmetic typo fixes across the prompt files) —
   * this invalidates every run cached during testing.
   */
  public static final String PROMPT_VERSION = "2";

  private AiSummaryConfig() {}
}
