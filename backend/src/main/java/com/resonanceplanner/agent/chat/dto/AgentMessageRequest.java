package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.agent.AgentLoop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new message to the agent: from 1 to 1,000 characters (D61). */
public record AgentMessageRequest(
        @NotBlank(message = "Scrivi un messaggio.")
        @Size(max = AgentLoop.MAX_MESSAGE_CHARS,
                message = "Il messaggio può avere al massimo " + AgentLoop.MAX_MESSAGE_CHARS + " caratteri.")
        String content) {}
