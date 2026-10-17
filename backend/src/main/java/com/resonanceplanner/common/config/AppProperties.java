package com.resonanceplanner.common.config;

import com.resonanceplanner.common.validation.MaxUtf8Bytes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * Required configuration, read from environment variables (D83).
 * If a value is missing, the application stops at startup and the message names the variable.
 * Records that hold passwords or secrets hide them in toString(), so they never end up in the logs.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Database database,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Agent agent,
        @DefaultValue RateLimit rateLimit) {

    private static final String HIDDEN = "***";

    public record Database(
            @NotBlank(message = "set the DATABASE_URL environment variable") String url,
            @NotBlank(message = "set the DATABASE_USERNAME environment variable") String username,
            @NotBlank(message = "set the DATABASE_PASSWORD environment variable") String password) {

        @Override
        public String toString() {
            return "Database[url=" + url + ", username=" + username + ", password=" + HIDDEN + "]";
        }
    }

    public record Jwt(
            @NotBlank(message = "set the JWT_SECRET environment variable")
            @Size(min = 32, message = "JWT_SECRET must be at least 32 characters (256 bits, D45)")
            String secret) {

        @Override
        public String toString() {
            return "Jwt[secret=" + HIDDEN + "]";
        }
    }

    public record Admin(
            @NotBlank(message = "set the ADMIN_EMAIL environment variable")
            @Email(message = "ADMIN_EMAIL must be a valid email address")
            String email,
            @NotBlank(message = "set the ADMIN_PASSWORD environment variable")
            @MaxUtf8Bytes(value = 72, message = "ADMIN_PASSWORD must be at most 72 bytes (BCrypt limit)")
            String password) {

        @Override
        public String toString() {
            return "Admin[email=" + email + ", password=" + HIDDEN + "]";
        }
    }

    // Real origins such as https://planner.example.com: no "*" and no path (D48)
    public record Cors(
            @NotEmpty(message = "set the CORS_ALLOWED_ORIGINS environment variable")
            List<@NotBlank(message = "CORS_ALLOWED_ORIGINS must not contain empty origins")
                    @Pattern(regexp = "^https?://[^/*\\s]+$",
                            message = "CORS_ALLOWED_ORIGINS must list origins like https://example.com, without * or paths")
                    String> allowedOrigins) {}

    /**
     * The model of the agent on OpenRouter (D126). It must be free: an id like "author/model:free", so a paid model,
     * or the router that picks a different free model every time, stops the application at startup.
     * A key pasted with a space or a line break would make every call fail, so it stops the startup too.
     */
    public record Agent(
            @NotBlank(message = "set the OPENROUTER_API_KEY environment variable")
            @Pattern(regexp = API_KEY, message = "OPENROUTER_API_KEY must not contain spaces or line breaks")
            String apiKey,
            @NotBlank(message = "set the OPENROUTER_MODEL environment variable")
            @Pattern(regexp = FREE_MODEL,
                    message = "OPENROUTER_MODEL must be a free model, with an id like author/model:free (D126)")
            String model) {

        public static final String API_KEY = "^\\S+$";
        public static final String FREE_MODEL = "^[\\w.-]+/[\\w.-]+:free$";

        @Override
        public String toString() {
            return "Agent[apiKey=" + HIDDEN + ", model=" + model + "]";
        }
    }

    /**
     * Limits of requests per client address (D48, D101). Not required: without properties the values of D48 apply.
     * They can be changed with the environment variables APP_RATELIMIT_AUTHATTEMPTS and the like, for example
     * to give more room on the day of the demo, when many people may share one network address.
     * A window written as a bare number counts minutes (authentication) or seconds (general): a bare number would
     * otherwise mean milliseconds, and "15" would silently switch the protection off.
     */
    public record RateLimit(
            @DefaultValue("10") int authAttempts,
            @DefaultValue("15") @DurationUnit(ChronoUnit.MINUTES) Duration authWindow,
            @DefaultValue("120") int generalRequests,
            @DefaultValue("60") @DurationUnit(ChronoUnit.SECONDS) Duration generalWindow) {

        private static final int MAX_REQUESTS = 1_000_000;
        private static final Duration MIN_WINDOW = Duration.ofSeconds(1);
        private static final Duration MAX_WINDOW = Duration.ofHours(24);

        public RateLimit {
            requireRequests("app.rate-limit.auth-attempts", authAttempts);
            requireWindow("app.rate-limit.auth-window", authWindow);
            requireRequests("app.rate-limit.general-requests", generalRequests);
            requireWindow("app.rate-limit.general-window", generalWindow);
        }

        private static void requireRequests(String property, int value) {
            if (value < 1 || value > MAX_REQUESTS) {
                throw new IllegalArgumentException(property + " must be between 1 and " + MAX_REQUESTS);
            }
        }

        private static void requireWindow(String property, Duration value) {
            if (value == null || value.compareTo(MIN_WINDOW) < 0 || value.compareTo(MAX_WINDOW) > 0) {
                throw new IllegalArgumentException(property + " must be between 1 second and 24 hours");
            }
        }
    }
}
