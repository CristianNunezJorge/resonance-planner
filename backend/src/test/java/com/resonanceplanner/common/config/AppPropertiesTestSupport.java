package com.resonanceplanner.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Shared fixtures for the tests that start a small context with AppProperties. */
final class AppPropertiesTestSupport {

    static final String[] VALID_PROPERTIES = {
        "app.database.url=jdbc:postgresql://localhost:5433/resonance_planner",
        "app.database.username=resonance",
        "app.database.password=database-password",
        "app.jwt.secret=a-test-secret-that-is-long-enough-for-jwt",
        "app.admin.email=admin@example.com",
        "app.admin.password=admin-password",
        "app.cors.allowed-origins=http://localhost:5173,https://planner.example.com",
        "app.agent.api-key=test-openrouter-key",
        "app.agent.model=google/gemma-4-31b-it:free"
    };

    private AppPropertiesTestSupport() {}

    @EnableConfigurationProperties(AppProperties.class)
    static class EnableAppProperties {}
}
