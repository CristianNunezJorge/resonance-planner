package com.resonanceplanner.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The guided setup (D20, D50) through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OnboardingControllerTest {

    private static final String URL = "/api/me/onboarding";
    private static final String JINHSI = "{\"characterId\":\"jinhsi\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";
    private static final String AALTO = "{\"characterId\":\"aalto\",\"currentLevel\":20,\"currentRank\":1,\"targetLevel\":40,\"targetRank\":1}";
    private static final String WAVEPLATE = "{\"value\":180,\"crystals\":0}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsers() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savesTheWholeSetupAndAnswersWithTheUpdatedUser() throws Exception {
        mockMvc.perform(as(rover, post(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(rover.getId()))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.playTimeMinutes").value(60))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(as(rover, get("/api/me")))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.playTimeMinutes").value(60));
        mockMvc.perform(as(rover, get("/api/me/roster")))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data[0].targetRank").value(2))
                .andExpect(jsonPath("$.data[1].characterId").value("aalto"))
                .andExpect(jsonPath("$.data[1].currentLevel").value(20));
        mockMvc.perform(as(rover, get("/api/me/waveplate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(180))
                .andExpect(jsonPath("$.data.crystals").value(0));
    }

    @Test
    void takesNoCrystalsAsZeroAndAcceptsThemWhileTheWaveplateAreNotFull() throws Exception {
        mockMvc.perform(as(rover, send(request("[" + JINHSI + "]", "{\"value\":100}", "30"))))
                .andExpect(status().isOk());
        mockMvc.perform(as(rover, get("/api/me/waveplate"))).andExpect(jsonPath("$.data.crystals").value(0));

        mockMvc.perform(as(other, send(request("[" + JINHSI + "]", "{\"value\":100,\"crystals\":300}", "120"))))
                .andExpect(status().isOk());
        mockMvc.perform(as(other, get("/api/me/waveplate"))).andExpect(jsonPath("$.data.crystals").value(300));
    }

    @Test
    void answersConflictWhenTheSetupIsAlreadyCompleteAndChangesNothing() throws Exception {
        mockMvc.perform(as(rover, send(validRequest()))).andExpect(status().isOk());

        mockMvc.perform(as(rover, send(request("[" + JINHSI + "]", "{\"value\":10}", "30"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("La configurazione è già stata completata."));

        mockMvc.perform(as(rover, get("/api/me/roster"))).andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(as(rover, get("/api/me"))).andExpect(jsonPath("$.data.playTimeMinutes").value(60));
        mockMvc.perform(as(rover, get("/api/me/waveplate"))).andExpect(jsonPath("$.data.value").value(180));
    }

    private static Stream<Arguments> requestsThatAreNotValid() {
        String chars = "[" + JINHSI + "," + AALTO + "]";
        String aaltoAbove = AALTO.replace("\"currentLevel\":20", "\"currentLevel\":5");
        return Stream.of(
                Arguments.of("no character", request("[]", WAVEPLATE, "60"), "characters"),
                Arguments.of("characters missing", request(null, WAVEPLATE, "60"), "characters"),
                Arguments.of("a null instead of a character", request("[null]", WAVEPLATE, "60"), "characters[0]"),
                Arguments.of("the same character twice", request("[" + JINHSI + "," + JINHSI + "]", WAVEPLATE, "60"), "characters[1].characterId"),
                Arguments.of("a character not in the catalog", request("[" + JINHSI.replace("jinhsi", "nobody") + "]", WAVEPLATE, "60"), "characters[0].characterId"),
                Arguments.of("level 5 after an ascension", request("[" + JINHSI + "," + aaltoAbove + "]", WAVEPLATE, "60"), "characters[1].currentLevel"),
                Arguments.of("goal lower than the present", request("[" + AALTO.replace("\"currentLevel\":20", "\"currentLevel\":30").replace("\"targetLevel\":40", "\"targetLevel\":25") + "]", WAVEPLATE, "60"), "characters[0].targetLevel"),
                Arguments.of("a missing level", request("[" + JINHSI.replace("\"currentLevel\":1,", "") + "]", WAVEPLATE, "60"), "characters[0].currentLevel"),
                Arguments.of("waveplate above the maximum", request(chars, "{\"value\":241}", "60"), "waveplate.value"),
                Arguments.of("negative waveplate", request(chars, "{\"value\":-1}", "60"), "waveplate.value"),
                Arguments.of("crystals above the maximum", request(chars, "{\"value\":240,\"crystals\":481}", "60"), "waveplate.crystals"),
                Arguments.of("waveplate missing", request(chars, null, "60"), "waveplate"),
                Arguments.of("waveplate value missing", request(chars, "{\"crystals\":0}", "60"), "waveplate.value"),
                Arguments.of("a play time that is not an option", request(chars, WAVEPLATE, "45"), "playTimeMinutes"),
                Arguments.of("play time missing", request(chars, WAVEPLATE, null), "playTimeMinutes"),
                Arguments.of("a level that is not a whole number", request("[" + JINHSI.replace("\"currentLevel\":1", "\"currentLevel\":1.5") + "]", WAVEPLATE, "60"), null),
                Arguments.of("a character that is not an identifier", request("[" + JINHSI.replace("jinhsi", "a\\u0000b") + "]", WAVEPLATE, "60"), "characters[0].characterId"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsThatAreNotValid")
    void refusesARequestThatIsNotValidAndSavesNothing(String description, String body, String field) throws Exception {
        var result = mockMvc.perform(as(rover, send(body))).andExpect(status().isBadRequest());
        if (field != null) {
            result.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields['" + field + "']").isNotEmpty());
        }

        assertNothingWasSaved(rover);
    }

    @Test
    void savesNothingWhenOnlyTheLastCharacterIsWrong() throws Exception {
        String wrong = AALTO.replace("\"currentLevel\":20", "\"currentLevel\":5");

        mockMvc.perform(as(rover, send(request("[" + JINHSI + "," + wrong + "]", WAVEPLATE, "60"))))
                .andExpect(status().isBadRequest());

        // The first character was fine, but it is not in the roster: all or nothing (D50)
        assertNothingWasSaved(rover);
    }

    @Test
    void listsEveryWrongFieldInTheOrderOfTheForm() throws Exception {
        String wrongChar = JINHSI.replace("\"currentLevel\":1", "\"currentLevel\":21").replace("jinhsi", "nobody");
        String body = request("[" + wrongChar + "]", "{\"value\":241,\"crystals\":481}", "45");

        String answer = mockMvc.perform(as(rover, send(body))).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        Map<String, String> fields = JsonPath.read(answer, "$.error.fields");
        // Characters first, then the Waveplate, then the play time: the order of the three steps
        assertThat(fields.keySet()).containsExactly("characters[0].characterId", "characters[0].currentLevel",
                "waveplate.value", "waveplate.crystals", "playTimeMinutes");
    }

    @Test
    void refusesAnAbsurdNumberOfCharacters() throws Exception {
        String many = "[" + String.join(",", java.util.Collections.nCopies(51, JINHSI)) + "]";

        mockMvc.perform(as(rover, send(request(many, WAVEPLATE, "60")))).andExpect(status().isBadRequest());

        assertNothingWasSaved(rover);
    }

    @Test
    void replacesTheCharactersThatTheUserAddedBeforeAndKeepsTheInventory() throws Exception {
        // Someone who used the API directly before the setup: the setup is the first, complete picture of the roster
        mockMvc.perform(as(rover, post("/api/me/roster").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"characterId\":\"jinhsi\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":20,\"targetRank\":0}")))
                .andExpect(status().isCreated());
        mockMvc.perform(as(rover, post("/api/me/roster").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"characterId\":\"yinlin\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":20,\"targetRank\":0}")))
                .andExpect(status().isCreated());
        mockMvc.perform(as(rover, put("/api/me/inventory/shell-credit").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":500}")))
                .andExpect(status().isOk());

        mockMvc.perform(as(rover, send(request("[" + JINHSI + "]", WAVEPLATE, "60")))).andExpect(status().isOk());

        mockMvc.perform(as(rover, get("/api/me/roster")))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data[0].targetLevel").value(40));
        mockMvc.perform(as(rover, get("/api/me/inventory")))
                .andExpect(jsonPath("$.data[0].materialId").value("shell-credit"))
                .andExpect(jsonPath("$.data[0].quantity").value(500));
    }

    @Test
    void neverTouchesTheDataOfAnotherUser() throws Exception {
        mockMvc.perform(as(other, send(validRequest()))).andExpect(status().isOk());

        mockMvc.perform(as(rover, send(request("[" + JINHSI + "]", "{\"value\":10}", "30")))).andExpect(status().isOk());

        mockMvc.perform(as(other, get("/api/me/roster"))).andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(as(other, get("/api/me/waveplate"))).andExpect(jsonPath("$.data.value").value(180));
        mockMvc.perform(as(rover, get("/api/me/roster"))).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void ignoresAnUserOrAFlagSentInTheBody() throws Exception {
        String body = "{\"characters\":[" + JINHSI + "],\"waveplate\":" + WAVEPLATE
                + ",\"playTimeMinutes\":60,\"userId\":" + other.getId() + ",\"onboardingCompleted\":false,\"role\":\"ADMIN\"}";

        mockMvc.perform(as(rover, send(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true));

        mockMvc.perform(as(other, get("/api/me"))).andExpect(jsonPath("$.data.onboardingCompleted").value(false));
        mockMvc.perform(as(other, get("/api/me/roster"))).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        mockMvc.perform(send(validRequest()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private void assertNothingWasSaved(User user) throws Exception {
        mockMvc.perform(as(user, get("/api/me")))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.data.playTimeMinutes").doesNotExist());
        mockMvc.perform(as(user, get("/api/me/roster"))).andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(as(user, get("/api/me/waveplate"))).andExpect(status().isNotFound());
    }

    // Each part is a piece of JSON (or null to leave the field out), so a test can break exactly one of them
    private static String request(String characters, String waveplate, String playTimeMinutes) {
        StringBuilder json = new StringBuilder("{");
        String separator = "";
        if (characters != null) {
            json.append("\"characters\":").append(characters);
            separator = ",";
        }
        if (waveplate != null) {
            json.append(separator).append("\"waveplate\":").append(waveplate);
            separator = ",";
        }
        if (playTimeMinutes != null) {
            json.append(separator).append("\"playTimeMinutes\":").append(playTimeMinutes);
        }
        return json.append("}").toString();
    }

    private static String validRequest() {
        return request("[" + JINHSI + "," + AALTO + "]", WAVEPLATE, "60");
    }

    private static MockHttpServletRequestBuilder send(String json) {
        return post(URL).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }
}
