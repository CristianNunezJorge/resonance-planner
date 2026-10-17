package com.resonanceplanner.agent.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.common.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * The test call of T32 against the real OpenRouter, with the free model of OPENROUTER_MODEL. It is off by default,
 * so the build never calls the real API (D63): it runs only with OPENROUTER_LIVE_TEST=true and the two variables
 * of the .env file, and it spends 2 of the 50 free requests of the day.
 * It logs the replies of the model: the prompt is fixed here, so no text of a real chat ends up in the logs (D60).
 */
@EnabledIfEnvironmentVariable(named = "OPENROUTER_LIVE_TEST", matches = "true")
class OpenRouterClientLiveTest {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClientLiveTest.class);

    private static final ToolDefinition GET_TODAY = new ToolDefinition(
            "get_today",
            "Restituisce la data di oggi in formato ISO (AAAA-MM-GG).",
            Map.of("type", "object", "properties", Map.of()));

    @Test
    void theFreeModelAsksForAToolAndThenAnswersWithItsResult() {
        AppProperties.Agent settings =
                new AppProperties.Agent(System.getenv("OPENROUTER_API_KEY"), System.getenv("OPENROUTER_MODEL"));
        ModelClient client = AgentClientConfig.openRouterClient(settings, JsonMapper.builder().build());
        List<ModelMessage> messages = new ArrayList<>(List.of(new ModelMessage.User("Che giorno è oggi?")));
        String system = "Sei un assistente di prova. Per sapere la data usa sempre lo strumento get_today, "
                + "poi rispondi in italiano con una sola frase che contiene la data.";

        ModelReply first = client.send(new ModelRequest(system, List.of(GET_TODAY), messages));
        log.info("First reply: {}, tools {}, {}", first.finishReason(), first.toolCalls(), first.usage());

        assertThat(first.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);
        assertThat(first.toolCalls()).extracting(ToolCall::name).containsOnly("get_today");

        messages.add(first.asMessage());
        first.toolCalls().forEach(call ->
                messages.add(new ModelMessage.ToolResult(call.id(), "{\"today\":\"2026-10-17\"}")));
        ModelReply second = client.send(new ModelRequest(system, List.of(GET_TODAY), messages));
        log.info("Second reply: {}, \"{}\", {}", second.finishReason(), second.text(), second.usage());

        assertThat(second.finishReason()).isEqualTo(FinishReason.STOP);
        assertThat(second.toolCalls()).isEmpty();
        assertThat(second.text()).contains("17");
    }
}
