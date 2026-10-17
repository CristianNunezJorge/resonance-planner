package com.resonanceplanner.agent.tools;

import java.util.Objects;

/**
 * The result of a tool for the model: a JSON text, marked as an error when the tool could not run, so the model can
 * correct itself (D61). A tool that ran also gives the step to show in the chat (D62); an error gives none.
 */
public record ToolOutcome(String content, boolean error, String step) {

    public ToolOutcome {
        Objects.requireNonNull(content, "content");
        if (error == (step != null)) {
            throw new IllegalArgumentException("A result has a step if and only if it is not an error");
        }
    }

    static ToolOutcome success(String content, String step) {
        return new ToolOutcome(content, false, Objects.requireNonNull(step, "step"));
    }

    static ToolOutcome failure(String content) {
        return new ToolOutcome(content, true, null);
    }
}
