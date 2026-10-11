package com.resonanceplanner.common.config;

import static com.resonanceplanner.common.config.AppPropertiesTestSupport.VALID_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        "app.cors.allowed-origins, CORS_ALLOWED_ORIGINS"
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
        "'app.cors.allowed-origins=,', CORS_ALLOWED_ORIGINS",
        "app.cors.allowed-origins=*, CORS_ALLOWED_ORIGINS",
        "app.cors.allowed-origins=https://planner.example.com/app, CORS_ALLOWED_ORIGINS"
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
                    .doesNotContain("database-password", "a-test-secret-that-is-long-enough-for-jwt", "admin-password");
        });
    }
}
