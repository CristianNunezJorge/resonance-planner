package com.resonanceplanner.agent.client;

import java.util.Objects;

/**
 * The model asks to run a tool. The arguments are the JSON text written by the model: the tools read and check
 * them like the body of an API request (D55), because the model can write anything.
 */
public record ToolCall(String id, String name, String arguments) {

    public ToolCall {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A tool call needs an id");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A tool call needs the name of the tool");
        }
        Objects.requireNonNull(arguments, "arguments");
    }
}
