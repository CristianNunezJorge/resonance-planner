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
        });
    }

    @Test
    void readsTheRateLimitPropertiesWhenTheyAreSet() {
        contextRunner.withPropertyValues(VALID_PROPERTIES)
                .withPropertyValues("app.rate-limit.auth-attempts=50", "app.rate-limit.auth-window=30m",
                        "app.rate-limit.general-requests=600", "app.rate-limit.general-window=10s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppProperties.RateLimit limits = context.getBean(AppProperties.class).rateLimit();
                    assertThat(limits.authAttempts()).isEqualTo(50);
                    assertThat(limits.authWindow()).isEqualTo(Duration.ofMinutes(30));
                    assertThat(limits.generalRequests()).isEqualTo(600);
                    assertThat(limits.generalWindow()).isEqualTo(Duration.ofSeconds(10));
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

    @ParameterizedTest
    @ValueSource(strings = {
        "app.rate-limit.auth-attempts=0",
        "app.rate-limit.auth-attempts=1000001",
        "app.rate-limit.general-requests=-5",
        "app.rate-limit.auth-window=0s",
        "app.rate-limit.auth-window=500ms",
        "app.rate-limit.auth-window=25h",
        "app.rate-limit.general-window=-1m",
        "app.rate-limit.general-window=PT99999999999H"
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
