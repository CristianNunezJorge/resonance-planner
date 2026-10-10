package com.resonanceplanner.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
        @Valid @NotNull Cors cors) {

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
            @NotBlank(message = "set the ADMIN_PASSWORD environment variable") String password) {

        @Override
        public String toString() {
            return "Admin[email=" + email + ", password=" + HIDDEN + "]";
        }
    }

    public record Cors(
            @NotEmpty(message = "set the CORS_ALLOWED_ORIGINS environment variable")
            List<@NotBlank(message = "CORS_ALLOWED_ORIGINS must not contain empty origins") String> allowedOrigins) {}
}
