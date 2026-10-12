package com.resonanceplanner.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// The limiter inside the real filter chain, with small limits (application-test.yml keeps them very high for the
// other tests). Every test uses its own address, because the counters live as long as the application context.
@SpringBootTest(properties = {"app.rate-limit.auth-attempts=3", "app.rate-limit.general-requests=10"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    // The frontend origin of application-test.yml
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersTooManyRequestsInTheApiFormatToTheAttemptAfterTheLimit() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(wrongLogin("203.0.113.10")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(wrongLogin("203.0.113.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.error.message").value("Troppe richieste: riprova tra 15 minuti."));
    }

    @Test
    void countsTheSuccessfulAttemptsToo() throws Exception {
        // The admin of application-test.yml: a correct login uses an attempt like a wrong one
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(login("203.0.113.11", "admin@example.com", "test-only-admin-password"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(login("203.0.113.11", "admin@example.com", "test-only-admin-password"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void limitsTheRegistrationWithItsOwnCounter() throws Exception {
        // An empty body is a valid attempt for the limiter (400 from the validation) and creates no account
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/api/auth/register").with(from("203.0.113.18"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/register").with(from("203.0.113.18"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
        // The login of the same client still has all its attempts
        mockMvc.perform(wrongLogin("203.0.113.18")).andExpect(status().isUnauthorized());
    }

    @Test
    void leavesOtherClientsFree() throws Exception {
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(wrongLogin("203.0.113.12"));
        }

        mockMvc.perform(wrongLogin("203.0.113.13")).andExpect(status().isUnauthorized());
    }

    @Test
    void keepsTheCorsHeadersOnTheRefusalSoTheBrowserCanReadTheMessage() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(wrongLogin("203.0.113.14"));
        }

        mockMvc.perform(wrongLogin("203.0.113.14").header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                // Without this the browser hides Retry-After from the frontend
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.RETRY_AFTER));
    }

    @Test
    void appliesTheGeneralLimitToTheRestOfTheApi() throws Exception {
        for (int request = 1; request <= 10; request++) {
            mockMvc.perform(get("/api/game/rules").with(from("203.0.113.15"))).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/game/rules").with(from("203.0.113.15")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void doesNotCountTheCorsPreflightRequestsOfTheBrowser() throws Exception {
        for (int request = 1; request <= 30; request++) {
            mockMvc.perform(options("/api/auth/login").with(from("203.0.113.16"))
                            .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(wrongLogin("203.0.113.16")).andExpect(status().isUnauthorized());
    }

    @Test
    void leavesRoomForTheHealthChecksOfTheHostingService() throws Exception {
        // The general limit of this test is 10: the health check has a much larger counter of its own
        for (int request = 1; request <= 30; request++) {
            mockMvc.perform(get("/actuator/health").with(from("203.0.113.17"))).andExpect(status().isOk());
        }
    }

    private static MockHttpServletRequestBuilder wrongLogin(String address) {
        return login(address, "nobody@example.com", "wrong-password");
    }

    private static MockHttpServletRequestBuilder login(
            String address, String email, String password) {
        return post("/api/auth/login").with(from(address)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
