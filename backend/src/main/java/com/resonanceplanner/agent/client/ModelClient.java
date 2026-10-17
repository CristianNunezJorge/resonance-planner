package com.resonanceplanner.agent.client;

/**
 * Sends one request to the language model and returns its reply (D54, D126). The agent loop talks only to this
 * interface: OpenRouterClient calls the real API, and the tests of the loop use a fake one without network (D63).
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
