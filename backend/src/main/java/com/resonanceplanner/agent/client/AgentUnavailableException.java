package com.resonanceplanner.agent.client;

import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ErrorCode;

/**
 * The model did not give a usable answer (502, D61). The message is for the user, so it never contains the
 * details of OpenRouter: those go to the logs. It always points to the basic plan, which works without AI (D64).
 */
public class AgentUnavailableException extends ApiException {

    static final String UNAVAILABLE_MESSAGE =
            "L'assistente non è disponibile in questo momento: riprova più tardi o usa il piano base.";
    static final String RATE_LIMITED_MESSAGE =
            "L'assistente gratuito ha raggiunto il limite di richieste: riprova più tardi o usa il piano base.";

    private AgentUnavailableException(String message) {
        super(ErrorCode.AGENT_UNAVAILABLE, message);
    }

    static AgentUnavailableException unavailable() {
        return new AgentUnavailableException(UNAVAILABLE_MESSAGE);
    }

    /** OpenRouter answered 429: the free requests of the minute or of the day are over (D126). */
    static AgentUnavailableException rateLimited() {
        return new AgentUnavailableException(RATE_LIMITED_MESSAGE);
    }
}
