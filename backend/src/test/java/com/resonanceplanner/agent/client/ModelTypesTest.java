package com.resonanceplanner.agent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The neutral types between the agent and the model client: they hold only valid, unchangeable values. */
class ModelTypesTest {

    private static final List<ModelMessage> ONE_MESSAGE = List.of(new ModelMessage.User("Ciao"));

    @Test
    void aRequestNeedsTheSystemPromptAndAtLeastOneMessage() {
        assertThatThrownBy(() -> new ModelRequest(" ", List.of(), ONE_MESSAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelRequest("Istruzioni", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRequestKeepsItsOwnCopyOfToolsAndMessages() {
        List<ModelMessage> messages = new ArrayList<>(ONE_MESSAGE);
        ModelRequest request = new ModelRequest("Istruzioni", List.of(), messages);

        messages.add(new ModelMessage.User("Altro"));

        assertThat(request.messages()).hasSize(1);
        assertThatThrownBy(() -> request.messages().add(new ModelMessage.User("Altro")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aToolNeedsANameADescriptionAndTheSchemaOfItsParameters() {
        assertThatThrownBy(() -> new ToolDefinition("", "Descrizione", Map.of("type", "object")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolDefinition("get_events", " ", Map.of("type", "object")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolDefinition("get_events", "Descrizione", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aToolCallNeedsAnIdAndAName() {
        assertThatThrownBy(() -> new ToolCall(" ", "get_events", "{}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolCall("call_1", "", "{}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolCall("call_1", "get_events", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void messagesHoldTextAndNeverNull() {
        assertThatThrownBy(() -> new ModelMessage.User(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModelMessage.Assistant(null, List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModelMessage.ToolResult(" ", "{}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelMessage.ToolResult("call_1", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anAssistantMessageKeepsItsOwnCopyOfTheToolCalls() {
        List<ToolCall> calls = new ArrayList<>(List.of(new ToolCall("call_1", "get_calendar", "{}")));
        ModelMessage.Assistant message = new ModelMessage.Assistant("", calls);

        calls.clear();

        assertThat(message.toolCalls()).hasSize(1);
    }

    @Test
    void tokensAreNeverNegative() {
        assertThatThrownBy(() -> new TokenUsage(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenUsage(0, -1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenUsage(0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(TokenUsage.NONE).isEqualTo(new TokenUsage(0, 0, 0));
    }

    @Test
    void tokensOfManyCallsAddUp() {
        assertThat(new TokenUsage(100, 20, 80).plus(new TokenUsage(50, 5, 0))).isEqualTo(new TokenUsage(150, 25, 80));
        assertThat(TokenUsage.NONE.plus(TokenUsage.NONE)).isEqualTo(TokenUsage.NONE);
        // Numbers of the provider near the largest int stop there instead of failing the turn
        assertThat(new TokenUsage(Integer.MAX_VALUE, 1, 0).plus(new TokenUsage(5, 1, 0)))
                .isEqualTo(new TokenUsage(Integer.MAX_VALUE, 2, 0));
    }

    @Test
    void aReplyKeepsItsOwnCopyOfTheToolCalls() {
        List<ToolCall> calls = new ArrayList<>(List.of(new ToolCall("call_1", "get_calendar", "{}")));
        ModelReply reply = new ModelReply("", calls, FinishReason.TOOL_CALLS, TokenUsage.NONE);

        calls.clear();

        assertThat(reply.toolCalls()).hasSize(1);
        assertThatThrownBy(() -> new ModelReply(null, List.of(), FinishReason.STOP, TokenUsage.NONE))
                .isInstanceOf(NullPointerException.class);
    }
}
