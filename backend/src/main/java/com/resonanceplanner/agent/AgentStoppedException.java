package com.resonanceplanner.agent;

import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ErrorCode;

/**
 * The model did not reach an answer: the 6 rounds are over, or the answer was cut, blocked or empty (D61, D128).
 * It is a 502 like the other failures of the agent, so the message is not saved and does not count in the limit.
 */
public class AgentStoppedException extends ApiException {

    static final String MESSAGE =
            "L'assistente non è riuscito a completare la richiesta: prova a riformularla in modo più semplice.";

    AgentStoppedException() {
        super(ErrorCode.AGENT_UNAVAILABLE, MESSAGE);
    }
}
