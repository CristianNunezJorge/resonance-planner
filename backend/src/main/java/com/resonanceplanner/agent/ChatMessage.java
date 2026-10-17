package com.resonanceplanner.agent;

import java.util.Objects;

/** A message of the saved chat, only its text (D40): the history the agent receives with a new message (D58). */
public record ChatMessage(Speaker speaker, String content) {

    public enum Speaker {
        USER,
        ASSISTANT
    }

    public ChatMessage {
        Objects.requireNonNull(speaker, "speaker");
        Objects.requireNonNull(content, "content");
    }
}
