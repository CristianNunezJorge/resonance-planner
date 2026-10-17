package com.resonanceplanner.agent.chat.dto;

import com.resonanceplanner.planning.dto.ProposalResponse;
import java.util.List;

/**
 * The answer to a message (D52): the saved answer of the agent, the proposal it made or null, the steps it did
 * (D62) and the usage of the day after this message.
 */
public record AgentAnswerResponse(
        AgentMessageResponse message, ProposalResponse proposal, List<String> steps, AgentUsageResponse usage) {

    public AgentAnswerResponse {
        steps = List.copyOf(steps);
    }
}
