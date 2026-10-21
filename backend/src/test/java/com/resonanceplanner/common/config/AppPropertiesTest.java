package com.resonanceplanner.common.config;

import static com.resonanceplanner.common.config.AppPropertiesTestSupport.VALID_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(AppPropertiesTestSupport.EnableAppProperties.class);

    @Test
    void bindsEveryValueWhenAllVariablesAreSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES).run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.database().url())
                    .isEqualTo("jdbc:postgresql://localhost:5433/resonance_planner");
            assertThat(properties.admin().email()).isEqualTo("admin@example.com");
            assertThat(properties.cors().allowedOrigins())
                    .containsExactly("http://localhost:5173", "https://planner.example.com");
            assertThat(properties.agent().apiKey()).isEqualTo("test-openrouter-key");
            assertThat(properties.agent().model()).isEqualTo("google/gemma-4-31b-it:free");
        });
    }

    @Test
    void usesTheLimitsOfD48WhenTheRateLimitPropertiesAreNotSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES).run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties.RateLimit limits = context.getBean(AppProperties.class).rateLimit();
            assertThat(limits.authAttempts()).isEqualTo(10);
            assertThat(limits.authWindow()).isEqualTo(Duration.ofMinutes(15));
            assertThat(limits.generalRequests()).isEqualTo(120);
            assertThat(limits.generalWindow()).isEqualTo(Duration.ofMinutes(1));
            // Guests of D134: 5 every 15 minutes, the window of the authentication
            assertThat(limits.guestCreations()).isEqualTo(5);
        });
    }

    @Test
    void readsTheRateLimitPropertiesWhenTheyAreSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES)
                .withPropertyValues("app.rate-limit.auth-attempts=50", "app.rate-limit.auth-window=30m",
                        "app.rate-limit.general-requests=600", "app.rate-limit.general-window=10s",
                        "app.rate-limit.guest-creations=20")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppProperties.RateLimit limits = context.getBean(AppProperties.class).rateLimit();
                    assertThat(limits.authAttempts()).isEqualTo(50);
                    assertThat(limits.authWindow()).isEqualTo(Duration.ofMinutes(30));
                    assertThat(limits.generalRequests()).isEqualTo(600);
                    assertThat(limits.generalWindow()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(limits.guestCreations()).isEqualTo(20);
                });
    }

    @Test
    void readsAWindowWrittenAsABareNumberInMinutesOrSecondsAndNotInMilliseconds() {
        contextRunner.withPropertyValues(VALID_PROPERTIES)
                .withPropertyValues("app.rate-limit.auth-window=20", "app.rate-limit.general-window=30")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppProperties.RateLimit limits = context.getBean(AppProperties.class).rateLimit();
                    assertThat(limits.authWindow()).isEqualTo(Duration.ofMinutes(20));
                    assertThat(limits.generalWindow()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void usesTheLimitsOfD129WhenTheAgentLimitPropertiesAreNotSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES).run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties.AgentLimits limits = context.getBean(AppProperties.class).agentLimits();
            assertThat(limits.dailyMessages()).isEqualTo(5);
            assertThat(limits.dailyUserModelCalls()).isEqualTo(24);
            assertThat(limits.dailyModelCalls()).isEqualTo(40);
            assertThat(limits.concurrentTurns()).isEqualTo(2);
            assertThat(limits.dailyBasicPlans()).isEqualTo(20);
            // Guests of D134: 2 messages each, 18 calls for all of them, so 22 of the 40 stay for registered users
            assertThat(limits.guestDailyMessages()).isEqualTo(2);
            assertThat(limits.guestDailyModelCalls()).isEqualTo(18);
        });
    }

    @Test
    void readsTheAgentLimitPropertiesWhenTheyAreSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES)
                .withPropertyValues("app.agent-limits.daily-messages=8", "app.agent-limits.daily-user-model-calls=60",
                        "app.agent-limits.daily-model-calls=900",
                        "app.agent-limits.concurrent-turns=3", "app.agent-limits.daily-basic-plans=50",
                        "app.agent-limits.guest-daily-messages=3", "app.agent-limits.guest-daily-model-calls=100")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppProperties.AgentLimits limits = context.getBean(AppProperties.class).agentLimits();
                    assertThat(limits.dailyMessages()).isEqualTo(8);
                    assertThat(limits.dailyUserModelCalls()).isEqualTo(60);
                    assertThat(limits.dailyModelCalls()).isEqualTo(900);
                    assertThat(limits.concurrentTurns()).isEqualTo(3);
                    assertThat(limits.dailyBasicPlans()).isEqualTo(50);
                    assertThat(limits.guestDailyMessages()).isEqualTo(3);
                    assertThat(limits.guestDailyModelCalls()).isEqualTo(100);
                });
    }

    // Fewer than 6 calls a day would never let a whole turn start (D61, D129)
    @ParameterizedTest
    @ValueSource(strings = {
        "app.agent-limits.daily-messages=0",
        "app.agent-limits.daily-messages=100001",
        "app.agent-limits.daily-user-model-calls=5",
        "app.agent-limits.daily-model-calls=5",
        "app.agent-limits.daily-model-calls=100001",
        "app.agent-limits.concurrent-turns=0",
        "app.agent-limits.concurrent-turns=101",
        "app.agent-limits.daily-basic-plans=-1",
        "app.agent-limits.guest-daily-messages=0",
        "app.agent-limits.guest-daily-model-calls=5",
        // The guests share the calls of the app: they cannot have more than all of it
        "app.agent-limits.guest-daily-model-calls=41"
    })
    void stopsWhenAnAgentLimitIsOutOfTheAllowedRange(String invalidProperty) {
        contextRunner.withPropertyValues(VALID_PROPERTIES).withPropertyValues(invalidProperty).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining(invalidProperty.split("=")[0]);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "app.rate-limit.auth-attempts=0",
        "app.rate-limit.auth-attempts=1000001",
        "app.rate-limit.general-requests=-5",
        "app.rate-limit.auth-window=0s",
        "app.rate-limit.auth-window=500ms",
        "app.rate-limit.auth-window=25h",
        "app.rate-limit.general-window=-1m",
        "app.rate-limit.general-window=PT99999999999H",
        "app.rate-limit.guest-creations=0"
    })
    void stopsWhenARateLimitIsOutOfTheAllowedRange(String invalidProperty) {
        contextRunner.withPropertyValues(VALID_PROPERTIES).withPropertyValues(invalidProperty).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining(invalidProperty.split("=")[0]);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "app.database.url, DATABASE_URL",
        "app.database.username, DATABASE_USERNAME",
        "app.database.password, DATABASE_PASSWORD",
        "app.jwt.secret, JWT_SECRET",
        "app.admin.email, ADMIN_EMAIL",
        "app.admin.password, ADMIN_PASSWORD",
        "app.cors.allowed-origins, CORS_ALLOWED_ORIGINS",
        "app.agent.api-key, OPENROUTER_API_KEY",
        "app.agent.model, OPENROUTER_MODEL"
    })
    void stopsWithTheVariableNameWhenAValueIsMissing(String property, String variable) {
        contextRunner.withPropertyValues(VALID_PROPERTIES).withPropertyValues(property + "=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining(variable);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "app.admin.email=not-an-email, ADMIN_EMAIL",
        "app.jwt.secret=shorter-than-32-characters, JWT_SECRET",
        "app.admin.password=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, ADMIN_PASSWORD",
        "'app.cors.allowed-origins=,', CORS_ALLOWED_ORIGINS",
        "app.cors.allowed-origins=*, CORS_ALLOWED_ORIGINS",
        "app.cors.allowed-origins=https://planner.example.com/app, CORS_ALLOWED_ORIGINS",
        // The agent must be free (D126): a paid model, or the router that picks a free model at random, stops the app
        "app.agent.model=anthropic/claude-haiku-4.5, OPENROUTER_MODEL",
        "app.agent.model=openrouter/free, OPENROUTER_MODEL",
        "app.agent.model=google/gemma-4-31b-it:free-trial, OPENROUTER_MODEL",
        "'app.agent.model=google/gemma 4:free', OPENROUTER_MODEL",
        "app.agent.model=google/gemma-4-31b-it:nitro:free, OPENROUTER_MODEL",
        // A key pasted with a space would make every call fail with an invalid header
        "'app.agent.api-key=sk-or-v1-abc def', OPENROUTER_API_KEY"
    })
    void stopsWithTheVariableNameWhenAValueIsInvalid(String invalidProperty, String variable) {
        contextRunner.withPropertyValues(VALID_PROPERTIES).withPropertyValues(invalidProperty).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining(variable);
        });
    }

    @Test
    void hidesPasswordsAndSecretsWhenPrinted() {
        contextRunner.withPropertyValues(VALID_PROPERTIES).run(context -> {
            String printed = context.getBean(AppProperties.class).toString();
            assertThat(printed)
                    .contains("admin@example.com")
                    .contains("google/gemma-4-31b-it:free")
                    .doesNotContain("database-password", "a-test-secret-that-is-long-enough-for-jwt", "admin-password",
                            "test-openrouter-key");
        });
    }
}
