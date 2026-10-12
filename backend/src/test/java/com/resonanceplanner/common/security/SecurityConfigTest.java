package com.resonanceplanner.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Access rules of docs/05-api.md and the JWT filter (D45, D48, D49, D53)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    // The frontend origin of application-test.yml
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppProperties appProperties;

    @Test
    void answersUnauthorizedInTheApiFormatForProtectedPaths() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Devi effettuare l'accesso."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void answersForbiddenInTheApiFormatToAUserWithoutTheAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/events"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void keepsOnlyReadingOfTheCatalogPublic() throws Exception {
        mockMvc.perform(post("/api/game/characters"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(head("/api/game/rules"))
                .andExpect(status().isOk());
    }

    @Test
    void keepsTheAuthenticationPathsOpen() throws Exception {
        // Reaches the controller (400 for the empty body) instead of stopping at 401
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void opensOnlyRegistrationAndLoginAmongTheAuthenticationPaths() throws Exception {
        mockMvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/another-path").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsTheBearerSchemeInAnyLetterCaseAndExtraSpacesAroundTheToken() throws Exception {
        String token = jwtService.generateToken(5L, Role.USER);

        mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, "bearer " + token + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void identifiesTheUserFromTheTokenOfTheAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(5L, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void answersForbiddenToAUserTokenOnAdminPathsAndOkToAnAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(5L, Role.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(1L, Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void answersUnauthorizedToAnExpiredGarbledOrWrongTypeTokenOnProtectedPaths() throws Exception {
        for (String authorization : new String[] {bearer(expiredToken()), "Bearer garbage", "Basic dXNlcjpwYXNz", "Bearer "}) {
            mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void keepsThePublicCatalogUsableEvenWithAnExpiredOrGarbledToken() throws Exception {
        for (String authorization : new String[] {bearer(expiredToken()), "Bearer garbage", "Basic dXNlcjpwYXNz"}) {
            mockMvc.perform(get("/api/game/rules").header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void keepsSwaggerAndTheHealthCheckPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void allowsCrossOriginRequestsFromTheFrontend() throws Exception {
        mockMvc.perform(options("/api/game/characters")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));
    }

    @Test
    void rejectsCrossOriginRequestsFromOtherSites() throws Exception {
        mockMvc.perform(options("/api/game/characters")
                        .header(HttpHeaders.ORIGIN, "https://other-site.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // Created two days ago with the secret of the application, so it is valid except for the date
    private String expiredToken() {
        Clock twoDaysAgo = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        return new JwtService(appProperties.jwt().secret(), twoDaysAgo).generateToken(5L, Role.ADMIN);
    }
}
