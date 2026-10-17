package com.resonanceplanner.agent.client;

/** Why the model stopped writing: the agent loop decides what to do next from this (D61). */
public enum FinishReason {
    /** The answer is complete. */
    STOP,
    /** The model asks to run one or more tools. */
    TOOL_CALLS,
    /** The answer was cut at the limit of output tokens. */
    LENGTH,
    /** The provider blocked the answer. */
    CONTENT_FILTER,
    /** A reason the client does not know. */
    OTHER
}
