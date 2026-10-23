package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.agent.ChatMessage.Speaker;
import com.resonanceplanner.agent.chat.AgentMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** A message of the chat: role USER or ASSISTANT, and the proposal it produced, if any (D40). */
public record AgentMessageResponse(
        @Schema(example = "101") Long id,
        @Schema(example = "ASSISTANT") Speaker role,
        @Schema(description = "Il testo, da mostrare come testo semplice",
                example = "Ho preparato il piano per la settimana dal 22 al 28 ottobre e l'ho salvato come proposta "
                        + "nel calendario.")
        String content,
        @Schema(description = "La proposta nata da questo messaggio; null se non c'è", nullable = true, example = "15")
        Long proposalId,
        @Schema(example = "2026-10-22T14:05:00Z") Instant createdAt) {

    public static AgentMessageResponse from(AgentMessage message) {
        return new AgentMessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getProposalId(), message.getCreatedAt());
    }
}
