package com.resonanceplanner.agent.client;

import java.util.List;
import java.util.Objects;

/** The reply of the model to one call: a text, the tools it asks for, or both. */
public record ModelReply(String text, List<ToolCall> toolCalls, FinishReason finishReason, TokenUsage usage) {

    public ModelReply {
        Objects.requireNonNull(text, "text");
        toolCalls = List.copyOf(toolCalls);
        Objects.requireNonNull(finishReason, "finishReason");
        Objects.requireNonNull(usage, "usage");
    }

    /** This reply as a message of the conversation, to send back in the next call of the loop. */
    public ModelMessage.Assistant asMessage() {
        return new ModelMessage.Assistant(text, toolCalls);
    }
}
