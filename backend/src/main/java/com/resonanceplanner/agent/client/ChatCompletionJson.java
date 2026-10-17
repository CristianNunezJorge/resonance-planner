package com.resonanceplanner.agent.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The JSON of the Chat Completions API of OpenRouter, in the format compatible with OpenAI (D126). Only the fields
 * we use: the replies ignore every other key, because OpenRouter and the providers add new ones over time.
 */
final class ChatCompletionJson {

    private ChatCompletionJson() {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record Request(
            String model,
            List<Message> messages,
            List<Tool> tools,
            @JsonProperty("max_tokens") int maxTokens,
            Provider provider) {}

    /** Only providers that support every parameter of the request, tools included. */
    record Provider(@JsonProperty("require_parameters") boolean requireParameters) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ToolCallJson> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolCallJson(String id, String type, FunctionCall function) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FunctionCall(String name, String arguments) {}

    record Tool(String type, FunctionDefinition function) {}

    record FunctionDefinition(String name, String description, Map<String, Object> parameters) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(List<Choice> choices, Usage usage, ErrorDetail error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason, ErrorDetail error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("prompt_tokens_details") PromptTokensDetails promptTokensDetails) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptTokensDetails(@JsonProperty("cached_tokens") Integer cachedTokens) {}

    /** The body of an error, with the HTTP status of the error or inside a reply with status 200. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(ErrorDetail error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorDetail(Object code, String message) {}
}
