package com.resonanceplanner.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Access rules of docs/05-api.md before the JWT login exists (D45, D48, D49, D53)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    // The frontend origin of application-test.yml
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

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
        // No login endpoint yet (T12): an open path answers 404 instead of 401
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isNotFound());
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
}
