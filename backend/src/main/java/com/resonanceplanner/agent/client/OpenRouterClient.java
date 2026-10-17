package com.resonanceplanner.agent.client;

import com.resonanceplanner.agent.client.ChatCompletionJson.Choice;
import com.resonanceplanner.agent.client.ChatCompletionJson.ErrorBody;
import com.resonanceplanner.agent.client.ChatCompletionJson.ErrorDetail;
import com.resonanceplanner.agent.client.ChatCompletionJson.FunctionCall;
import com.resonanceplanner.agent.client.ChatCompletionJson.FunctionDefinition;
import com.resonanceplanner.agent.client.ChatCompletionJson.Message;
import com.resonanceplanner.agent.client.ChatCompletionJson.Provider;
import com.resonanceplanner.agent.client.ChatCompletionJson.Request;
import com.resonanceplanner.agent.client.ChatCompletionJson.Response;
import com.resonanceplanner.agent.client.ChatCompletionJson.Tool;
import com.resonanceplanner.agent.client.ChatCompletionJson.ToolCallJson;
import com.resonanceplanner.agent.client.ChatCompletionJson.Usage;
import com.resonanceplanner.common.config.AppProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The model client for OpenRouter (D126): one POST to the Chat Completions API, in the format compatible with
 * OpenAI. It turns our types into the JSON of the API and back, and every failure into AgentUnavailableException
 * (502, D61), so the agent loop never sees an HTTP or JSON error.
 * The logs name the status and the error code of OpenRouter, never the text of the chat (D60): the message of an
 * error can repeat part of the prompt, so it is logged only at DEBUG level, which is off by default.
 */
public class OpenRouterClient implements ModelClient {

    static final String COMPLETIONS_URL = "https://openrouter.ai/api/v1/chat/completions";
    // A chat answer is short; the cap also keeps the reply under the read limits of Jackson (D110, D126)
    static final int MAX_OUTPUT_TOKENS = 2048;
    // A real reply is a few KB: a bigger one is read no further, so it cannot fill the 512 MB of the free server
    static final int MAX_REPLY_BYTES = 1_000_000;

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final Pattern FREE_MODEL = Pattern.compile(AppProperties.Agent.FREE_MODEL);
    private static final Pattern API_KEY = Pattern.compile(AppProperties.Agent.API_KEY);
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\p{Cntrl}\\u0085\\u2028\\u2029]");
    private static final int MAX_LOGGED_DETAIL = 200;
    private static final String RATE_LIMITED_CODE = String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value());

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    // Each call runs in a virtual thread, so it can be stopped when its time is over
    private final ExecutorService calls = Executors.newVirtualThreadPerTaskExecutor();

    public OpenRouterClient(RestClient restClient, JsonMapper jsonMapper, String apiKey, String model,
            Duration timeout) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        // The configuration already checks both values at startup; these checks cover code that builds the client
        if (apiKey == null || !API_KEY.matcher(apiKey).matches()) {
            throw new IllegalArgumentException("The API key must be one word, without spaces or line breaks");
        }
        if (model == null || !FREE_MODEL.matcher(model).matches()) {
            throw new IllegalArgumentException("The model must be free, with an id like author/model:free (D126)");
        }
        this.apiKey = apiKey;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public Duration timeout() {
        return timeout;
    }

    /**
     * The whole call, body of the reply included, has {@code timeout} to finish (60 seconds, D61). The timeout of
     * the HTTP client is not enough: it stops waiting for the headers, but not a server that sends the headers and
     * then keeps quiet.
     */
    @Override
    public ModelReply send(ModelRequest request) {
        byte[] body = requestBody(request);
        Future<byte[]> call = calls.submit(() -> post(body));
        try {
            return fromJson(call.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            call.cancel(true);
            log.warn("OpenRouter did not answer within {} ms for model {}", timeout.toMillis(), model);
            throw AgentUnavailableException.unavailable();
        } catch (ExecutionException e) {
            throw failureOf(e.getCause());
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw AgentUnavailableException.unavailable();
        }
    }

    private byte[] post(byte[] body) {
        return restClient.post()
                .uri(COMPLETIONS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange(this::readReply);
    }

    private AgentUnavailableException failureOf(Throwable cause) {
        if (cause instanceof AgentUnavailableException unavailable) {
            return unavailable;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        // No connection, or any other failure of the call: the class says which, its message may hold the address
        log.warn("OpenRouter did not answer for model {}: {}", model, cause.getClass().getSimpleName());
        return AgentUnavailableException.unavailable();
    }

    private byte[] requestBody(ModelRequest request) {
        try {
            return jsonMapper.writeValueAsBytes(toJson(request));
        } catch (JacksonException e) {
            // A tool schema that cannot become JSON is a mistake in our code, not in OpenRouter
            log.error("Could not write the request for model {}: {}", model, e.getClass().getSimpleName());
            throw AgentUnavailableException.unavailable();
        }
    }

    // Reads at most MAX_REPLY_BYTES, also for errors: an error status becomes AgentUnavailableException here
    private byte[] readReply(HttpRequest clientRequest, ConvertibleClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        byte[] content = response.getBody().readNBytes(MAX_REPLY_BYTES + 1);
        if (content.length > MAX_REPLY_BYTES) {
            throw unusable("a reply bigger than " + MAX_REPLY_BYTES + " bytes, with status " + status);
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw failed(status, content);
        }
        return content;
    }

    private Request toJson(ModelRequest request) {
        List<Message> messages = Stream.concat(
                        Stream.of(new Message("system", request.systemPrompt(), null, null)),
                        request.messages().stream().map(OpenRouterClient::toJson))
                .toList();
        List<Tool> tools = request.tools().stream()
                .map(tool -> new Tool("function",
                        new FunctionDefinition(tool.name(), tool.description(), tool.parameters())))
                .toList();
        return new Request(model, messages, tools, MAX_OUTPUT_TOKENS, new Provider(true));
    }

    private static Message toJson(ModelMessage message) {
        return switch (message) {
            case ModelMessage.User user -> new Message("user", user.text(), null, null);
            case ModelMessage.Assistant assistant -> toJson(assistant);
            case ModelMessage.ToolResult result -> new Message("tool", result.content(), null, result.toolCallId());
        };
    }

    private static Message toJson(ModelMessage.Assistant assistant) {
        if (assistant.toolCalls().isEmpty()) {
            return new Message("assistant", assistant.text(), null, null);
        }
        List<ToolCallJson> calls = assistant.toolCalls().stream()
                .map(call -> new ToolCallJson(call.id(), "function", new FunctionCall(call.name(), call.arguments())))
                .toList();
        // A turn with only tool calls has no text at all, as in the format of OpenAI
        String text = assistant.text().isBlank() ? null : assistant.text();
        return new Message("assistant", text, calls, null);
    }

    private ModelReply fromJson(byte[] body) {
        if (body == null || body.length == 0) {
            throw unusable("an empty reply");
        }
        Response response;
        try {
            response = jsonMapper.readValue(body, Response.class);
        } catch (JacksonException e) {
            throw unusable("a reply that is not valid JSON");
        }
        if (response == null) {
            throw unusable("an empty reply");
        }
        if (response.error() != null) {
            throw providerError(response.error());
        }
        if (response.choices() == null || response.choices().isEmpty() || response.choices().getFirst() == null) {
            throw unusable("a reply without choices");
        }
        Choice choice = response.choices().getFirst();
        if (choice.error() != null || "error".equals(choice.finishReason())) {
            throw providerError(choice.error());
        }
        Message message = choice.message();
        if (message == null) {
            throw unusable("a reply without a message");
        }
        List<ToolCall> calls = toolCalls(message.toolCalls());
        String text = message.content() == null ? "" : message.content();
        // Some providers say "stop" even when they ask for tools: the tool calls decide
        FinishReason reason = calls.isEmpty() ? finishReason(choice.finishReason()) : FinishReason.TOOL_CALLS;
        return new ModelReply(text, calls, reason, usage(response.usage()));
    }

    private List<ToolCall> toolCalls(List<ToolCallJson> calls) {
        if (calls == null) {
            return List.of();
        }
        return calls.stream().map(this::toolCall).toList();
    }

    private ToolCall toolCall(ToolCallJson call) {
        FunctionCall function = call == null ? null : call.function();
        if (function == null || function.name() == null || function.name().isBlank()) {
            throw unusable("a tool call without a name");
        }
        // Some free providers leave out the id: the loop needs one to link each result to its call
        String id = call.id() == null || call.id().isBlank() ? "call_" + UUID.randomUUID() : call.id();
        String arguments = function.arguments() == null || function.arguments().isBlank() ? "{}" : function.arguments();
        return new ToolCall(id, function.name(), arguments);
    }

    private static FinishReason finishReason(String reason) {
        if (reason == null) {
            return FinishReason.OTHER;
        }
        return switch (reason) {
            case "stop" -> FinishReason.STOP;
            case "tool_calls" -> FinishReason.TOOL_CALLS;
            case "length" -> FinishReason.LENGTH;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.OTHER;
        };
    }

    private static TokenUsage usage(Usage usage) {
        if (usage == null) {
            return TokenUsage.NONE;
        }
        int cached = usage.promptTokensDetails() == null ? 0 : countOf(usage.promptTokensDetails().cachedTokens());
        return new TokenUsage(countOf(usage.promptTokens()), countOf(usage.completionTokens()), cached);
    }

    private static int countOf(Integer tokens) {
        return tokens == null || tokens < 0 ? 0 : tokens;
    }

    private AgentUnavailableException failed(int status, byte[] body) {
        ErrorDetail error = errorOf(body);
        log.warn("OpenRouter answered {} for model {}, error code {}", status, model, codeOf(error));
        log.debug("Message of the OpenRouter error: {}", messageOf(error));
        return status == HttpStatus.TOO_MANY_REQUESTS.value()
                ? AgentUnavailableException.rateLimited()
                : AgentUnavailableException.unavailable();
    }

    private AgentUnavailableException providerError(ErrorDetail error) {
        log.warn("OpenRouter reported an error for model {}, error code {}", model, codeOf(error));
        log.debug("Message of the OpenRouter error: {}", messageOf(error));
        return RATE_LIMITED_CODE.equals(codeOf(error))
                ? AgentUnavailableException.rateLimited()
                : AgentUnavailableException.unavailable();
    }

    private AgentUnavailableException unusable(String what) {
        log.warn("OpenRouter sent {} for model {}", what, model);
        return AgentUnavailableException.unavailable();
    }

    /** The error in a body of OpenRouter, or null if the body has none. */
    private ErrorDetail errorOf(byte[] body) {
        try {
            ErrorBody errorBody = jsonMapper.readValue(body, ErrorBody.class);
            return errorBody == null ? null : errorBody.error();
        } catch (JacksonException e) {
            return null;
        }
    }

    private static String codeOf(ErrorDetail error) {
        return error == null || error.code() == null ? "none" : clean(String.valueOf(error.code()));
    }

    private static String messageOf(ErrorDetail error) {
        return error == null || error.message() == null ? "none" : clean(error.message());
    }

    // One short line in the logs, whatever the text contains
    private static String clean(String text) {
        String line = LINE_BREAKS.matcher(text).replaceAll(" ");
        return line.length() <= MAX_LOGGED_DETAIL ? line : line.substring(0, MAX_LOGGED_DETAIL) + "...";
    }
}
