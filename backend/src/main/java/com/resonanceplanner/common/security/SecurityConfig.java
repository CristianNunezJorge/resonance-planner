package com.resonanceplanner.common.security;

import com.resonanceplanner.auth.JwtAuthenticationFilter;
import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.notification.NotificationSocketConfig;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Access rules of docs/05-api.md and JWT login (D45): the token of the Authorization header identifies the user.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    // Game catalog and events: readable without login (D49)
    private static final String[] PUBLIC_READ_PATHS = {"/api/game/**", "/api/events/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityErrorHandler errorHandler,
            JwtService jwtService, AppProperties appProperties, Clock clock) throws Exception {
        http
                // The token travels in the Authorization header, never in a cookie: no session and no CSRF (D45)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Logging out means forgetting the token in the browser (D45): the default /logout would only
                // answer a pointless redirect, before the rate limits
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // HEAD too, because uptime monitors use it
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_PATHS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_READ_PATHS).permitAll()
                        // Only these three (the guest of D134 too): a future path under /api/auth must not become
                        // public by accident
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/guest")
                        .permitAll()
                        // The WebSocket of the notifications: the browser cannot send the token when it opens it,
                        // so the token arrives in the first message and the handler checks it (D135)
                        .requestMatchers(HttpMethod.GET, NotificationSocketConfig.PATH).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                // Both filters are created here and not as beans: a Filter bean would also be registered by
                // Spring Boot, and run twice. The limits come first, so a refused request costs almost nothing
                // (no token to check, no body to read), and after CorsFilter, so the 429 keeps the CORS headers.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(appProperties.rateLimit(), clock, errorHandler),
                        JwtAuthenticationFilter.class);
        return http.build();
    }

    // The JWT secret is read from the environment (D83); the clock comes from ClockConfig
    @Bean
    public JwtService jwtService(AppProperties appProperties, Clock clock) {
        return new JwtService(appProperties.jwt().secret(), clock);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Only the frontend of CORS_ALLOWED_ORIGINS may call the API from a browser (D48)
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // The browser hides the other response headers from the frontend: this one tells when to try again (D101)
        configuration.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
