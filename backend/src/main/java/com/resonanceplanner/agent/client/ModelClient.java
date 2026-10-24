package com.resonanceplanner.agent.client;

/**
 * Sends one request to the language model and returns its reply (D54, D126). The agent loop talks only to this
 * interface, and OpenRouterClient calls the real API: the loop does not depend on the provider.
 */
public interface ModelClient {

    /**
     * One call to the model, with the whole conversation of the turn.
     *
     * @throws AgentUnavailableException if the model does not answer, answers with an error or with a reply that
     *     cannot be used (502, D61)
     */
    ModelReply send(ModelRequest request);
}
