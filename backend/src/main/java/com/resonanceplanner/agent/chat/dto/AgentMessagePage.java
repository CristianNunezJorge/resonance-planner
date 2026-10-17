package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.common.api.PageMeta;
import java.util.List;

/** One page of the history of the chat, from the newest message, with its meta part (D46). */
public record AgentMessagePage(List<AgentMessageResponse> messages, PageMeta meta) {

    public AgentMessagePage {
        messages = List.copyOf(messages);
    }
}
