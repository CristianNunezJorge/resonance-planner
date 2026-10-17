package com.resonanceplanner.agent.client;

import java.util.List;
import java.util.Objects;

/** A message of the conversation with the model: the user, the model itself, or the result of a tool. */
public sealed interface ModelMessage {

    /** A message of the user, or the history of the chat (D58). */
    record User(String text) implements ModelMessage {

        public User {
            Objects.requireNonNull(text, "text");
        }
    }

    /** A reply of the model, sent back in the next call so the model sees what it asked for. */
    record Assistant(String text, List<ToolCall> toolCalls) implements ModelMessage {

        public Assistant {
            Objects.requireNonNull(text, "text");
            toolCalls = List.copyOf(toolCalls);
        }
    }

    /** The result of one tool call, linked to it by its id; errors too are results the model can read (D61). */
    record ToolResult(String toolCallId, String content) implements ModelMessage {

        public ToolResult {
            if (toolCallId == null || toolCallId.isBlank()) {
                throw new IllegalArgumentException("A tool result needs the id of its call");
            }
            Objects.requireNonNull(content, "content");
        }
    }
}
