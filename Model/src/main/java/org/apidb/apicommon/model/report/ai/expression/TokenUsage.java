package org.apidb.apicommon.model.report.ai.expression;

/**
 * Represents token usage for AI API calls, including chat completions and embeddings.
 * Immutable value object with builder pattern.
 */
public class TokenUsage {

  private final long promptTokens;
  private final long completionTokens;
  private final long embeddingTokens;

  private TokenUsage(Builder builder) {
    this.promptTokens = builder.promptTokens;
    this.completionTokens = builder.completionTokens;
    this.embeddingTokens = builder.embeddingTokens;
  }

  public long promptTokens() {
    return promptTokens;
  }

  public long completionTokens() {
    return completionTokens;
  }

  public long embeddingTokens() {
    return embeddingTokens;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long promptTokens = 0;
    private long completionTokens = 0;
    private long embeddingTokens = 0;

    public Builder promptTokens(long promptTokens) {
      this.promptTokens = promptTokens;
      return this;
    }

    public Builder completionTokens(long completionTokens) {
      this.completionTokens = completionTokens;
      return this;
    }

    public Builder embeddingTokens(long embeddingTokens) {
      this.embeddingTokens = embeddingTokens;
      return this;
    }

    public TokenUsage build() {
      return new TokenUsage(this);
    }
  }
}
