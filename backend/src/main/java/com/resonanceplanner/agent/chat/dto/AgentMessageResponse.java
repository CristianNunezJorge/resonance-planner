package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.agent.ChatMessage.Speaker;
import com.resonanceplanner.agent.chat.AgentMessage;
import java.time.Instant;

/** A message of the chat: role USER or ASSISTANT, and the proposal it produced, if any (D40). */
public record AgentMessageResponse(Long id, Speaker role, String content, Long proposalId, Instant createdAt) {

    public static AgentMessageResponse from(AgentMessage message) {
        return new AgentMessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getProposalId(), message.getCreatedAt());
    }
}
