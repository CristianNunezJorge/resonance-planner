package com.resonanceplanner.agent;

import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.planning.dto.ProposalResponse;
import java.util.List;
import java.util.Objects;

/**
 * The answer of the agent to one message (D52): the text for the user, the proposal saved in the turn or null, the
 * steps it did (D62), and for the logs the tokens and the calls to the model of the whole turn.
 */
public record AgentReply(String text, ProposalResponse proposal, List<String> steps, TokenUsage usage, int rounds) {

    public AgentReply {
        Objects.requireNonNull(text, "text");
        steps = List.copyOf(steps);
        Objects.requireNonNull(usage, "usage");
    }
}
