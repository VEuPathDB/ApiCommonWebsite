package org.apidb.apicommon.model.report.ai.expression;

/**
 * Represents token usage for AI API calls, including chat completions and embeddings.
 * Immutable value object with builder pattern.
 */
public class TokenUsage {

  private final long _promptTokens;
  private final long _completionTokens;
  private final long _embeddingTokens;

  private TokenUsage(Builder builder) {
    _promptTokens = builder._promptTokens;
    _completionTokens = builder._completionTokens;
    _embeddingTokens = builder._embeddingTokens;
  }

  public long promptTokens() {
    return _promptTokens;
  }

  public long completionTokens() {
    return _completionTokens;
  }

  public long embeddingTokens() {
    return _embeddingTokens;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long _promptTokens = 0;
    private long _completionTokens = 0;
    private long _embeddingTokens = 0;

    public Builder promptTokens(long promptTokens) {
      _promptTokens = promptTokens;
      return this;
    }

    public Builder completionTokens(long completionTokens) {
      _completionTokens = completionTokens;
      return this;
    }

    public Builder embeddingTokens(long embeddingTokens) {
      _embeddingTokens = embeddingTokens;
      return this;
    }

    public TokenUsage build() {
      return new TokenUsage(this);
    }
  }
}
