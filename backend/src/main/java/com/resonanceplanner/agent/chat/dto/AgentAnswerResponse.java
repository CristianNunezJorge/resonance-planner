package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.planning.dto.ProposalResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The answer to a message (D52): the saved answer of the agent, the proposal it made or null, the steps it did
 * (D62) and the usage of the day after this message.
 */
public record AgentAnswerResponse(
        AgentMessageResponse message,
        @Schema(description = "La proposta fatta in questo messaggio; null se non c'è", nullable = true,
                types = {"object", "null"})
        ProposalResponse proposal,
        @Schema(description = "I passaggi fatti dall'assistente",
                example = "[\"Ho letto il tuo roster e le Waveplate\", \"Ho calcolato il piano\", "
                        + "\"Ho preparato la proposta\"]")
        List<String> steps,
        AgentUsageResponse usage) {

    public AgentAnswerResponse {
        steps = List.copyOf(steps);
    }
}
