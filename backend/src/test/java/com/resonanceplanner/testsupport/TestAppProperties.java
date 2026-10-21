package com.resonanceplanner.testsupport;

import com.resonanceplanner.common.config.AppProperties;
import java.time.Duration;
import java.util.List;

/** AppProperties for the unit tests: fixed values that are not secrets, with the limits a test chooses. */
public final class TestAppProperties {

    private TestAppProperties() {}

    public static AppProperties withAgentLimits(AppProperties.AgentLimits agentLimits) {
        return new AppProperties(
                new AppProperties.Database("jdbc:postgresql://localhost:5433/test", "user", "password"),
                new AppProperties.Jwt("0123456789abcdef0123456789abcdef"),
                new AppProperties.Admin("admin@example.com", "admin-password"),
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Agent("test-openrouter-key", "test/model:free"),
                new AppProperties.RateLimit(10, Duration.ofMinutes(15), 120, Duration.ofMinutes(1), 5),
                agentLimits);
    }
}
