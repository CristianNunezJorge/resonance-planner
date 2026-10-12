package com.resonanceplanner.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Swagger gets the "Authorize" button to paste the JWT and try the protected endpoints (D53)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void describesTheBearerTokenOfTheLogin() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void asksForTheTokenOnlyOnTheProfileEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/me'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/me'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/me'].delete.security[0].bearerAuth").isArray())
                // The user of the token is not a parameter that the caller has to fill in
                .andExpect(jsonPath("$.paths['/api/me'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/game/rules'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist());
    }
}
