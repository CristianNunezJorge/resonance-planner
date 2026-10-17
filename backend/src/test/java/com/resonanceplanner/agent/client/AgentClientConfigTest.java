package com.resonanceplanner.agent.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.common.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

class AgentClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableAppProperties.class, AgentClientConfig.class)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withPropertyValues(
                    "app.database.url=jdbc:postgresql://localhost:5433/resonance_planner",
                    "app.database.username=resonance",
                    "app.database.password=database-password",
                    "app.jwt.secret=a-test-secret-that-is-long-enough-for-jwt",
                    "app.admin.email=admin@example.com",
                    "app.admin.password=admin-password",
                    "app.cors.allowed-origins=http://localhost:5173",
                    "app.agent.api-key=test-openrouter-key",
                    "app.agent.model=google/gemma-4-31b-it:free");

    @Test
    void theAgentTalksToOpenRouterWithTheConfiguredFreeModelAndAtMostSixtySecondsPerCall() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ModelClient.class)).isInstanceOfSatisfying(OpenRouterClient.class, client -> {
                assertThat(client.model()).isEqualTo("google/gemma-4-31b-it:free");
                assertThat(client.timeout()).isEqualTo(Duration.ofSeconds(60));
            });
        });
    }

    @Test
    void doesNotStartWithAPaidModel() {
        contextRunner.withPropertyValues("app.agent.model=anthropic/claude-haiku-4.5").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("OPENROUTER_MODEL");
        });
    }

    // Only the properties: the rest of the application is not needed to build the client
    @EnableConfigurationProperties(AppProperties.class)
    static class EnableAppProperties {}
}
