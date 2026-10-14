package com.resonanceplanner.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// The size of a JSON body is limited before a request is built from it (application.yml, spring.jackson.factory):
// @Size on a list runs only after Jackson has created every element, so a body of a few MB could fill the 512 MB of
// the free server. The unknown field "junk" is ignored by the mapper, so without the limits all these bodies would be
// accepted: that is how the tests tell the limit from a validation error.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JsonBodyLimitsTest {

    private static final String URL = "/api/me/waveplate";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User rover;

    @BeforeEach
    void createAUser() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
    }

    @Test
    void acceptsABodyWithManyMoreFieldsThanARealRequestHas() throws Exception {
        // 200 extra numbers: far above any real request, far below the limit
        String numbers = IntStream.range(0, 200).mapToObj(String::valueOf).collect(Collectors.joining(","));

        send("{\"value\":180,\"junk\":[" + numbers + "]}").andExpect(status().isOk());
    }

    @Test
    void refusesABodyWithTooManyElementsBeforeBuildingThem() throws Exception {
        String numbers = String.join(",", Collections.nCopies(20_000, "0"));

        send("{\"value\":180,\"junk\":[" + numbers + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Richiesta non valida."));
    }

    @Test
    void refusesADocumentThatIsTooLong() throws Exception {
        // Few tokens, but 8 strings of 15,000 characters: 120,000 characters in all
        String text = "x".repeat(15_000);
        String fields = IntStream.range(0, 8).mapToObj(i -> "\"junk" + i + "\":\"" + text + "\"").collect(Collectors.joining(","));

        send("{\"value\":180," + fields + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Richiesta non valida."));
    }

    private ResultActions send(String json) throws Exception {
        return mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(rover.getId(), rover.getRole())));
    }
}
