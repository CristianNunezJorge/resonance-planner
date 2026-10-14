package com.resonanceplanner.roster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The roster API through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RosterControllerTest {

    private static final String URL = "/api/me/roster";
    private static final String JINHSI = "{\"characterId\":\"jinhsi\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";
    private static final String AALTO = "{\"characterId\":\"aalto\",\"currentLevel\":20,\"currentRank\":1,\"targetLevel\":40,\"targetRank\":1}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsers() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JINHSI)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(URL + "/jinhsi").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(URL + "/jinhsi")).andExpect(status().isUnauthorized());
    }

    @Test
    void startsEmpty() throws Exception {
        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void addsACharacterAndListsItWithItsLevelsAndGoal() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data.currentLevel").value(1))
                .andExpect(jsonPath("$.data.currentRank").value(0))
                .andExpect(jsonPath("$.data.targetLevel").value(40))
                .andExpect(jsonPath("$.data.targetRank").value(2));
        mockMvc.perform(as(rover, postWith(AALTO))).andExpect(status().isCreated());

        // In the order they were added
        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data[1].characterId").value("aalto"))
                .andExpect(jsonPath("$.data[1].currentLevel").value(20))
                .andExpect(jsonPath("$.data[1].currentRank").value(1));
    }

    @Test
    void answersConflictForACharacterThatIsAlreadyInTheRoster() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());

        mockMvc.perform(as(rover, postWith(JINHSI)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.fields.characterId").value("Questo personaggio è già nel tuo roster."));
        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void letsEachUserHaveTheSameCharacter() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        mockMvc.perform(as(other, postWith(JINHSI))).andExpect(status().isCreated());

        mockMvc.perform(as(other, get(URL))).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void refusesACharacterThatIsNotInTheCatalog() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI.replace("jinhsi", "nobody"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.characterId").value("Questo personaggio non esiste."));
    }

    private static Stream<Arguments> combinationsThatDoNotExist() {
        String template = "{\"characterId\":\"jinhsi\",\"currentLevel\":%d,\"currentRank\":%d,\"targetLevel\":%d,\"targetRank\":%d}";
        return Stream.of(
                // level 5 cannot be after an ascension
                Arguments.of(template.formatted(5, 1, 40, 2), "currentLevel"),
                // level 21 is above the maximum of rank 0
                Arguments.of(template.formatted(21, 0, 40, 2), "currentLevel"),
                Arguments.of(template.formatted(1, 0, 41, 2), "targetLevel"),
                Arguments.of(template.formatted(1, 3, 40, 2), "currentRank"),
                // the goal is lower than the present
                Arguments.of(template.formatted(30, 1, 25, 1), "targetLevel"),
                Arguments.of(template.formatted(30, 1, 20, 0), "targetRank"));
    }

    @ParameterizedTest
    @MethodSource("combinationsThatDoNotExist")
    void refusesCombinationsOfLevelAndRankThatDoNotExistAndSavesNothing(String body, String field) throws Exception {
        mockMvc.perform(as(rover, postWith(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields." + field).isNotEmpty());

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void listsEveryWrongFieldInTheSameAnswerInTheOrderOfTheForm() throws Exception {
        String body = mockMvc.perform(as(rover, postWith("{\"characterId\":\"nobody\",\"currentLevel\":21,\"currentRank\":0,\"targetLevel\":41,\"targetRank\":2}")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // Always the same order, whatever the JVM: the frontend can put the focus on the first wrong input
        Map<String, String> fields = JsonPath.read(body, "$.error.fields");
        assertThat(fields.keySet()).containsExactly("characterId", "currentLevel", "targetLevel");
    }

    @Test
    void refusesAnIdentifierThatCannotBeOneOfTheCatalogWithoutAnError() throws Exception {
        // A NUL character is valid JSON, but PostgreSQL refuses it in a text: it must be a 400 here, not a 500 later
        String[] identifiers = {"a\\u0000b", "Jinhsi", "jin hsi", "jinhsi/../x", "'; drop table users; --", "a".repeat(61)};
        for (String identifier : identifiers) {
            mockMvc.perform(as(rover, postWith(JINHSI.replace("jinhsi", identifier))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.characterId").value("Questo personaggio non esiste."));
        }
    }

    @Test
    void refusesMissingAndNotWholeValues() throws Exception {
        mockMvc.perform(as(rover, postWith("{}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.characterId").isNotEmpty())
                .andExpect(jsonPath("$.error.fields.currentLevel").isNotEmpty())
                .andExpect(jsonPath("$.error.fields.currentRank").isNotEmpty())
                .andExpect(jsonPath("$.error.fields.targetLevel").isNotEmpty())
                .andExpect(jsonPath("$.error.fields.targetRank").isNotEmpty());
        mockMvc.perform(as(rover, postWith(JINHSI.replace("\"currentLevel\":1", "\"currentLevel\":1.5"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, postWith(JINHSI.replace("\"currentLevel\":1", "\"currentLevel\":\"one\""))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, postWith(JINHSI.replace("\"jinhsi\"", "\"\""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesTheLevelsAndTheGoalOfACharacter() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());

        mockMvc.perform(as(rover, putWith("jinhsi", "{\"currentLevel\":20,\"currentRank\":1,\"targetLevel\":40,\"targetRank\":2}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data.currentLevel").value(20))
                .andExpect(jsonPath("$.data.currentRank").value(1));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data[0].currentLevel").value(20));
    }

    @Test
    void ignoresACharacterIdInTheBodyOfAnUpdate() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());

        mockMvc.perform(as(rover, putWith("jinhsi", AALTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterId").value("jinhsi"));
        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void refusesAnUpdateThatIsNotValidAndChangesNothing() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(as(rover, putWith("jinhsi", "{\"currentLevel\":5,\"currentRank\":1,\"targetLevel\":40,\"targetRank\":2}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.currentLevel").isNotEmpty());

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data[0].currentLevel").value(1))
                .andExpect(jsonPath("$.data[0].currentRank").value(0));
    }

    @Test
    void answersNotFoundForACharacterThatIsNotInTheRoster() throws Exception {
        String body = "{\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";

        mockMvc.perform(as(rover, putWith("jinhsi", body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(as(rover, delete(URL + "/jinhsi")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void neverTouchesTheRosterOfAnotherUser() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        String body = "{\"currentLevel\":20,\"currentRank\":1,\"targetLevel\":40,\"targetRank\":2}";

        // For the other user the character does not exist: 404, never 403 and never a change
        mockMvc.perform(as(other, putWith("jinhsi", body))).andExpect(status().isNotFound());
        mockMvc.perform(as(other, delete(URL + "/jinhsi"))).andExpect(status().isNotFound());

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].currentLevel").value(1));
    }

    @Test
    void takesTheCharacterOutOfTheRoster() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        mockMvc.perform(as(rover, postWith(AALTO))).andExpect(status().isCreated());

        mockMvc.perform(as(rover, delete(URL + "/jinhsi"))).andExpect(status().isNoContent());

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].characterId").value("aalto"));
        // And it can be added again
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
    }

    @Test
    void disappearsWithTheAccount() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        mockMvc.perform(as(other, postWith(JINHSI))).andExpect(status().isCreated());

        mockMvc.perform(as(rover, delete("/api/me"))).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from owned_character where user_id = ?", Integer.class, rover.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from owned_character where user_id = ?", Integer.class, other.getId())).isEqualTo(1);
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        mockMvc.perform(as(rover, postWith(JINHSI))).andExpect(status().isCreated());
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {get(URL), postWith(AALTO),
                putWith("jinhsi", "{\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}"),
                delete(URL + "/jinhsi")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }

    private static MockHttpServletRequestBuilder postWith(String json) {
        return post(URL).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder putWith(String characterId, String json) {
        return put(URL + "/" + characterId).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
