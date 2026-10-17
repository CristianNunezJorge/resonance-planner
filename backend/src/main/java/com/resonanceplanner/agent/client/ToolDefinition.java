package com.resonanceplanner.agent.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A tool the model can ask for (D55): its name, what it does, and the JSON Schema of its parameters.
 * The description is in Italian, like the instructions, because it tells the model when to use the tool.
 */
public record ToolDefinition(String name, String description, Map<String, Object> parameters) {

    public ToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A tool needs a name");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A tool needs a description");
        }
        // A copy that keeps the order of the keys: the same tools give the same JSON at every call (D58)
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters")));
    }
}
