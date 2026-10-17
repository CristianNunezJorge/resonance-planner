package com.resonanceplanner.agent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ErrorCode;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** The client of OpenRouter against a fake server: no network and no cost (D63, D126). */
class OpenRouterClientTest {

    private static final String MODEL = "google/gemma-4-31b-it:free";
    private static final String API_KEY = "test-openrouter-key";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final ToolDefinition GET_EVENTS = new ToolDefinition(
            "get_events",
            "Eventi attivi nell'intervallo",
            Map.of("type", "object", "properties", Map.of("from", Map.of("type", "string"))));

    private final Logger clientLog = (Logger) LoggerFactory.getLogger(OpenRouterClient.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private MockRestServiceServer server;
    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenRouterClient(builder.build(), JsonMapper.builder().build(), API_KEY, MODEL, TIMEOUT);
        logs.start();
        clientLog.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        clientLog.detachAppender(logs);
    }

    private static ModelRequest askOnce(String text) {
        return new ModelRequest("Sei l'assistente di prova.", List.of(), List.of(new ModelMessage.User(text)));
    }

    private void respond(String json) {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic/claude-haiku-4.5", "openrouter/free", ""})
    void refusesAModelThatIsNotFreeEvenWhenItIsBuiltByHand(String model) {
        // The configuration already stops the app (D126); the client checks again, for code that builds it directly
        assertThatThrownBy(() -> new OpenRouterClient(
                        RestClient.create(), JsonMapper.builder().build(), API_KEY, model, TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(":free");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sk-or-v1-abc def", "sk-or-v1-abc\n"})
    void refusesAKeyWithSpacesOrLineBreaks(String apiKey) {
        // A key like this would make every call fail with an invalid Authorization header
        assertThatThrownBy(() -> new OpenRouterClient(
                        RestClient.create(), JsonMapper.builder().build(), apiKey, MODEL, TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("sk-or");
    }

    @Test
    void sendsTheSystemPromptTheToolsAndTheWholeConversationInTheOpenAiFormat() {
        ToolCall call = new ToolCall("call_1", "get_events", "{\"from\":\"2026-10-17\"}");
        ModelRequest request = new ModelRequest(
                "Sei l'assistente di prova.",
                List.of(GET_EVENTS),
                List.of(
                        new ModelMessage.User("Quali eventi ci sono?"),
                        new ModelMessage.Assistant("", List.of(call)),
                        new ModelMessage.ToolResult("call_1", "{\"events\":[]}")));
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "google/gemma-4-31b-it:free",
                          "max_tokens": 2048,
                          "provider": {"require_parameters": true},
                          "messages": [
                            {"role": "system", "content": "Sei l'assistente di prova."},
                            {"role": "user", "content": "Quali eventi ci sono?"},
                            {"role": "assistant", "tool_calls": [
                              {"id": "call_1", "type": "function",
                               "function": {"name": "get_events", "arguments": "{\\"from\\":\\"2026-10-17\\"}"}}
                            ]},
                            {"role": "tool", "tool_call_id": "call_1", "content": "{\\"events\\":[]}"}
                          ],
                          "tools": [
                            {"type": "function", "function": {
                              "name": "get_events",
                              "description": "Eventi attivi nell'intervallo",
                              "parameters": {"type": "object", "properties": {"from": {"type": "string"}}}
                            }}
                          ]
                        }
                        """, true))
                // An assistant turn with only tool calls has no text, not an empty one
                .andExpect(jsonPath("$.messages[2].content").doesNotExist())
                .andRespond(withSuccess(textReply("Nessun evento.", "stop"), MediaType.APPLICATION_JSON));

        client.send(request);

        server.verify();
    }

    @Test
    void sendsATextTogetherWithTheToolCallsAndAToolWithoutParametersAsAnEmptySchema() {
        ToolDefinition getToday = new ToolDefinition("get_today", "Data di oggi",
                Map.of("type", "object", "properties", Map.of()));
        ModelRequest request = new ModelRequest("Sei l'assistente di prova.", List.of(getToday), List.of(
                new ModelMessage.User("Che giorno è?"),
                new ModelMessage.Assistant("Controllo la data.", List.of(new ToolCall("call_1", "get_today", "{}"))),
                new ModelMessage.ToolResult("call_1", "{\"today\":\"2026-10-17\"}")));
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andExpect(jsonPath("$.messages[2].content").value("Controllo la data."))
                .andExpect(jsonPath("$.messages[2].tool_calls[0].id").value("call_1"))
                .andExpect(content().json("""
                        {"tools": [{"type": "function", "function": {
                          "name": "get_today", "description": "Data di oggi",
                          "parameters": {"type": "object", "properties": {}}}}]}
                        """))
                .andRespond(withSuccess(textReply("È il 17 ottobre.", "stop"), MediaType.APPLICATION_JSON));

        client.send(request);

        server.verify();
    }

    @Test
    void sendsTheToolSchemaWithItsKeysInTheSameOrder() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("from", Map.of("type", "string")));
        schema.put("required", List.of("from"));
        ToolDefinition tool = new ToolDefinition("get_events", "Eventi", schema);
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
                        .contains("\"parameters\":{\"type\":\"object\",\"properties\":{\"from\""))
                .andRespond(withSuccess(textReply("Ok.", "stop"), MediaType.APPLICATION_JSON));

        client.send(new ModelRequest("Istruzioni", List.of(tool), List.of(new ModelMessage.User("Eventi?"))));

        server.verify();
    }

    @Test
    void answersThatTheAgentIsUnavailableWhenAToolSchemaCannotBecomeJson() {
        // A map that contains itself has no end as JSON
        Map<String, Object> endless = new HashMap<>();
        endless.put("items", endless);
        ToolDefinition broken = new ToolDefinition("get_events", "Eventi", Map.of("properties", endless));
        ModelRequest request = new ModelRequest("Istruzioni", List.of(broken), List.of(new ModelMessage.User("Eventi?")));

        assertThatThrownBy(() -> client.send(request))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @Test
    void leavesOutTheToolsWhenTheRequestHasNone() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andRespond(withSuccess(textReply("Ciao!", "stop"), MediaType.APPLICATION_JSON));

        client.send(askOnce("Ciao"));

        server.verify();
    }

    @Test
    void sendsAccentedLettersAsUtf8() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andExpect(request -> assertThat(new String(
                        ((MockClientHttpRequest) request).getBodyAsBytes(), StandardCharsets.UTF_8))
                        .contains("Perché è così?"))
                .andRespond(withSuccess(textReply("Perché sì.", "stop"), MediaType.APPLICATION_JSON));

        ModelReply reply = client.send(askOnce("Perché è così?"));

        assertThat(reply.text()).isEqualTo("Perché sì.");
    }

    @Test
    void readsATextAnswerWithItsTokens() {
        respond("""
                {"choices": [{"finish_reason": "stop", "message": {"role": "assistant", "content": "Ecco il piano."}}],
                 "usage": {"prompt_tokens": 1200, "completion_tokens": 85, "prompt_tokens_details": {"cached_tokens": 1000}}}
                """);

        ModelReply reply = client.send(askOnce("Organizzami la settimana"));

        assertThat(reply.text()).isEqualTo("Ecco il piano.");
        assertThat(reply.toolCalls()).isEmpty();
        assertThat(reply.finishReason()).isEqualTo(FinishReason.STOP);
        assertThat(reply.usage()).isEqualTo(new TokenUsage(1200, 85, 1000));
    }

    @Test
    void readsTheToolCallsInTheOrderTheModelAsksForThem() {
        respond("""
                {"choices": [{"finish_reason": "tool_calls", "message": {"role": "assistant", "content": null,
                  "tool_calls": [
                    {"id": "call_a", "type": "function", "function": {"name": "get_player_status", "arguments": "{}"}},
                    {"id": "call_b", "type": "function",
                     "function": {"name": "get_events", "arguments": "{\\"from\\":\\"2026-10-17\\"}"}}
                  ]}}]}
                """);

        ModelReply reply = client.send(askOnce("Organizzami la settimana"));

        assertThat(reply.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);
        assertThat(reply.text()).isEmpty();
        assertThat(reply.toolCalls()).containsExactly(
                new ToolCall("call_a", "get_player_status", "{}"),
                new ToolCall("call_b", "get_events", "{\"from\":\"2026-10-17\"}"));
        // The provider did not report the tokens
        assertThat(reply.usage()).isEqualTo(TokenUsage.NONE);
    }

    @Test
    void ignoresTheKeysOfTheReplyItDoesNotUse() {
        // Free providers add their own keys: a reasoning model, for example, also sends its reasoning
        respond("""
                {"id": "gen-123", "provider": "Google AI Studio", "model": "google/gemma-4-31b-it:free",
                 "object": "chat.completion", "created": 1792230000, "system_fingerprint": null,
                 "choices": [{"index": 0, "logprobs": null, "finish_reason": "stop", "native_finish_reason": "STOP",
                   "message": {"role": "assistant", "content": "Ecco.", "refusal": null, "reasoning": "Penso...",
                               "reasoning_details": [{"type": "reasoning.text", "text": "Penso..."}],
                               "annotations": []}}],
                 "usage": {"prompt_tokens": 10, "completion_tokens": 2, "total_tokens": 12, "cost": 0,
                           "prompt_tokens_details": {"cached_tokens": 0, "audio_tokens": 0},
                           "completion_tokens_details": {"reasoning_tokens": 1}}}
                """);

        ModelReply reply = client.send(askOnce("Ciao"));

        assertThat(reply.text()).isEqualTo("Ecco.");
        assertThat(reply.usage()).isEqualTo(new TokenUsage(10, 2, 0));
    }

    @Test
    void treatsAReplyWithToolCallsAsAToolCallReplyWhateverReasonItGives() {
        respond("""
                {"choices": [{"finish_reason": "stop", "message": {"role": "assistant", "content": "Controllo.",
                  "tool_calls": [{"id": "call_a", "type": "function",
                                  "function": {"name": "get_calendar", "arguments": "{}"}}]}}]}
                """);

        ModelReply reply = client.send(askOnce("Cosa faccio oggi?"));

        assertThat(reply.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);
        assertThat(reply.text()).isEqualTo("Controllo.");
        assertThat(reply.toolCalls()).extracting(ToolCall::name).containsExactly("get_calendar");
    }

    @Test
    void givesAnIdToAToolCallWithoutOneAndEmptyArgumentsWhenTheyAreMissing() {
        respond("""
                {"choices": [{"finish_reason": "tool_calls", "message": {"role": "assistant",
                  "tool_calls": [{"type": "function", "function": {"name": "get_player_status"}},
                                 {"id": "", "type": "function", "function": {"name": "get_calendar", "arguments": " "}}]}}]}
                """);

        ModelReply reply = client.send(askOnce("Come sto messo?"));

        assertThat(reply.toolCalls()).extracting(ToolCall::arguments).containsExactly("{}", "{}");
        assertThat(reply.toolCalls()).extracting(ToolCall::id)
                .allSatisfy(id -> assertThat(id).startsWith("call_").hasSizeGreaterThan(5))
                .doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @CsvSource({
        "length, LENGTH",
        "content_filter, CONTENT_FILTER",
        "something_new, OTHER"
    })
    void reportsWhyTheModelStopped(String reason, FinishReason expected) {
        respond(textReply("Il piano è", reason));

        assertThat(client.send(askOnce("Organizzami la settimana")).finishReason()).isEqualTo(expected);
    }

    @Test
    void readsAMissingTextAsAnEmptyOne() {
        respond("""
                {"choices": [{"finish_reason": "stop", "message": {"role": "assistant"}}]}
                """);

        assertThat(client.send(askOnce("Ciao")).text()).isEmpty();
    }

    @Test
    void turnsTheReplyIntoTheAssistantMessageOfTheNextRequest() {
        respond("""
                {"choices": [{"finish_reason": "tool_calls", "message": {"role": "assistant", "content": "",
                  "tool_calls": [{"id": "call_a", "type": "function", "function": {"name": "get_calendar", "arguments": "{}"}}]}}]}
                """);

        ModelReply reply = client.send(askOnce("Cosa faccio oggi?"));

        assertThat(reply.asMessage())
                .isEqualTo(new ModelMessage.Assistant("", List.of(new ToolCall("call_a", "get_calendar", "{}"))));
    }

    @Test
    void saysTheFreeLimitIsOverWhenOpenRouterAnswers429() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": 429, \"message\": \"Rate limit exceeded: free-models-per-day\"}}"));

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.RATE_LIMITED_MESSAGE)
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.AGENT_UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"error\": {\"code\": 429, \"message\": \"Rate limit exceeded\"}}",
        "{\"error\": {\"code\": \"429\", \"message\": \"Rate limit exceeded\"}}",
        "{\"choices\": [{\"finish_reason\": \"error\", \"error\": {\"code\": 429, \"message\": \"Upstream rate limit\"},"
                + " \"message\": {\"role\": \"assistant\", \"content\": \"\"}}]}"
    })
    void saysTheFreeLimitIsOverAlsoWhenThe429ArrivesInsideAReplyWithStatus200(String body) {
        respond(body);

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.RATE_LIMITED_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 400, 401, 402, 403, 404, 408, 500, 502, 503})
    void answersThatTheAgentIsUnavailableWhenOpenRouterAnswersWithAnError(int status) {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": " + status + ", \"message\": \"Something went wrong\"}}"));

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                // The user never sees the details of OpenRouter
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @Test
    void answersThatTheAgentIsUnavailableWhenAnErrorHasABodyThatIsNotJson() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML).body("<html>502</html>"));

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @Test
    void answersThatTheAgentIsUnavailableWhenTheReplyIsEmpty() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 500})
    void readsNoMoreThanOneMegabyteOfReply(int status) {
        byte[] huge = new byte[OpenRouterClient.MAX_REPLY_BYTES + 1];
        Arrays.fill(huge, (byte) ' ');
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(huge));

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @Test
    void neverWritesTheChatOrTheKeyInTheLogs() {
        // Some providers repeat the prompt in the message of an error (D60)
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": 400, \"message\": \"Flagged input: Il mio segreto\"}}"));

        assertThatThrownBy(() -> client.send(askOnce("Il mio segreto"))).isInstanceOf(AgentUnavailableException.class);

        assertThat(logs.list).isNotEmpty();
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(line -> assertThat(line).doesNotContain("Il mio segreto", API_KEY))
                .anySatisfy(line -> assertThat(line).contains("400"));
    }

    @Test
    void stopsTheWholeCallWhenItsTimeIsOver() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer slowServer = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient impatient =
                new OpenRouterClient(builder.build(), JsonMapper.builder().build(), API_KEY, MODEL, Duration.ofMillis(200));
        // The server answers after 3 seconds: the client gives up after 200 milliseconds and stops the call
        slowServer.expect(requestTo(OpenRouterClient.COMPLETIONS_URL)).andRespond(request -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("stopped");
            }
            return withSuccess(textReply("Troppo tardi.", "stop"), MediaType.APPLICATION_JSON).createResponse(request);
        });
        long start = System.nanoTime();

        assertThatThrownBy(() -> impatient.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void answersThatTheAgentIsUnavailableWhenTheCallFailsInAnUnexpectedWay() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL)).andRespond(request -> {
            throw new IllegalStateException("Unexpected");
        });

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @Test
    void answersThatTheAgentIsUnavailableWhenOpenRouterDoesNotAnswerInTime() {
        server.expect(requestTo(OpenRouterClient.COMPLETIONS_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not json",
        "{}",
        "{\"choices\": []}",
        "{\"choices\": [{\"finish_reason\": \"stop\"}]}",
        "null",
        "{\"choices\": [null]}",
        // The text must be a string: some formats send a list of parts, which this client does not read
        "{\"choices\": [{\"finish_reason\": \"stop\", \"message\": {\"role\": \"assistant\","
                + " \"content\": [{\"type\": \"text\"}]}}]}",
        // Errors of the provider can arrive with status 200, in the reply or in the choice
        "{\"error\": {\"code\": 502, \"message\": \"Provider returned error\"}}",
        "{\"choices\": [{\"finish_reason\": \"error\", \"message\": {\"role\": \"assistant\", \"content\": \"\"}}]}",
        "{\"choices\": [{\"finish_reason\": \"stop\", \"error\": {\"code\": 502, \"message\": \"Upstream error\"},"
                + " \"message\": {\"role\": \"assistant\", \"content\": \"\"}}]}",
        // A tool call without a name cannot be run
        "{\"choices\": [{\"finish_reason\": \"tool_calls\", \"message\": {\"role\": \"assistant\","
                + " \"tool_calls\": [{\"id\": \"call_a\", \"type\": \"function\", \"function\": {\"arguments\": \"{}\"}}]}}]}"
    })
    void answersThatTheAgentIsUnavailableWhenTheReplyCannotBeUsed(String body) {
        respond(body);

        assertThatThrownBy(() -> client.send(askOnce("Ciao")))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessage(AgentUnavailableException.UNAVAILABLE_MESSAGE);
    }

    private static String textReply(String text, String finishReason) {
        return """
                {"choices": [{"finish_reason": "%s", "message": {"role": "assistant", "content": "%s"}}]}
                """.formatted(finishReason, text);
    }
}
