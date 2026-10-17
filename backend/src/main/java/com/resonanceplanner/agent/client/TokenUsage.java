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

    /**
     * The tokens of this call and of another, for the total of a turn of the agent. The numbers come from the
     * provider: a sum beyond the largest int stays at the largest int, so it can never stop a turn.
     */
    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(sum(inputTokens, other.inputTokens), sum(outputTokens, other.outputTokens),
                sum(cachedInputTokens, other.cachedInputTokens));
    }

    private static int sum(int a, int b) {
        return (int) Math.min((long) a + b, Integer.MAX_VALUE);
    }
}
