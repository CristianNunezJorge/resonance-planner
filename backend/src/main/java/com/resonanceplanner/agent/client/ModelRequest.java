package com.resonanceplanner.agent.client;

import java.util.List;

/**
 * What the model receives at every call: the fixed parts first (instructions and tools, D58) and then the
 * conversation, with the tool calls and results of the turn so far.
 */
public record ModelRequest(String systemPrompt, List<ToolDefinition> tools, List<ModelMessage> messages) {

    public ModelRequest {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("A request needs the system prompt");
        }
        tools = List.copyOf(tools);
        messages = List.copyOf(messages);
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("A request needs at least one message");
        }
    }
}
