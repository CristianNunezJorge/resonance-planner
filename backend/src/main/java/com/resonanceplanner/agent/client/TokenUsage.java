package com.resonanceplanner.agent.client;

/**
 * The tokens of one call, for the logs (D60). The tokens read from the prompt cache are counted only if the
 * provider reports them: with the free models the cache is not guaranteed (D126).
 */
public record TokenUsage(int inputTokens, int outputTokens, int cachedInputTokens) {

    /** The provider did not report the tokens. */
    public static final TokenUsage NONE = new TokenUsage(0, 0, 0);

    public TokenUsage {
        if (inputTokens < 0 || outputTokens < 0 || cachedInputTokens < 0) {
            throw new IllegalArgumentException("Tokens cannot be negative");
        }
    }
}
