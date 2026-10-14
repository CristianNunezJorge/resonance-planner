package com.resonanceplanner.waveplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The Waveplate API through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WaveplateControllerTest {

    private static final String URL = "/api/me/waveplate";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

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
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"value\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersNotFoundUntilTheUserSavesTheirWaveplate() throws Exception {
        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void savesTheValuesReadInTheGameAndAnswersWithTheState() throws Exception {
        String body = mockMvc.perform(as(rover, putWith("{\"value\":180,\"crystals\":0}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value(180))
                .andExpect(jsonPath("$.data.crystals").value(0))
                .andExpect(jsonPath("$.data.max").value(240))
                .andExpect(jsonPath("$.data.crystalsMax").value(480))
                .andReturn().getResponse().getContentAsString();

        Instant recordedAt = Instant.parse(JsonPath.read(body, "$.data.recordedAt"));
        Instant fullAt = Instant.parse(JsonPath.read(body, "$.data.fullAt"));
        Instant crystalsFullAt = Instant.parse(JsonPath.read(body, "$.data.crystalsFullAt"));
        // The server chooses the moment of the reading, and it is "now"
        assertThat(Duration.between(recordedAt, Instant.now())).isLessThan(Duration.ofMinutes(1));
        // 60 Waveplate missing x 6 minutes; then 480 Crystals x 12 minutes
        assertThat(Duration.between(recordedAt, fullAt)).isEqualTo(Duration.ofMinutes(360));
        assertThat(Duration.between(fullAt, crystalsFullAt)).isEqualTo(Duration.ofMinutes(5760));
        assertThat(userRepository.findById(rover.getId()).orElseThrow().getWaveplate().value()).isEqualTo(180);
    }

    @Test
    void takesNoCrystalsAsZero() throws Exception {
        mockMvc.perform(as(rover, putWith("{\"value\":100}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(100))
                .andExpect(jsonPath("$.data.crystals").value(0));
    }

    @Test
    void acceptsCrystalsAlsoWhileTheWaveplateAreNotFull() throws Exception {
        // The Crystals stay when the Waveplate are spent (D105)
        mockMvc.perform(as(rover, putWith("{\"value\":100,\"crystals\":300}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(100))
                .andExpect(jsonPath("$.data.crystals").value(300));
        mockMvc.perform(as(rover, putWith("{\"value\":240,\"crystals\":300}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.crystals").value(300));
    }

    @Test
    void ignoresAMomentOfTheReadingSentByTheClient() throws Exception {
        String body = mockMvc.perform(as(rover, putWith("{\"value\":10,\"recordedAt\":\"2020-01-01T00:00:00Z\"}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Instant recordedAt = Instant.parse(JsonPath.read(body, "$.data.recordedAt"));
        assertThat(Duration.between(recordedAt, Instant.now())).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void answersTheSameInstantsToThePutAndToTheNextGet() throws Exception {
        String put = mockMvc.perform(as(rover, putWith("{\"value\":100}"))).andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        String get = mockMvc.perform(as(rover, get(URL))).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(get, "$.data.recordedAt")).isEqualTo(JsonPath.read(put, "$.data.recordedAt"));
        assertThat((String) JsonPath.read(get, "$.data.fullAt")).isEqualTo(JsonPath.read(put, "$.data.fullAt"));
    }

    @Test
    void calculatesTheCurrentValueFromTheMomentOfTheReading() throws Exception {
        // Read 63 minutes ago: 10 steps of 6 minutes, with margin for the time the test takes
        rover.recordWaveplate(new WaveplateSnapshot(100, 0, Instant.now().minus(Duration.ofMinutes(63))));
        userRepository.saveAndFlush(rover);

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(110))
                .andExpect(jsonPath("$.data.crystals").value(0));
    }

    @Test
    void showsTheCrystalsThatAccumulatedAfterTheWaveplateGotFull() throws Exception {
        // Full since 123 minutes: 10 Crystals (12 minutes each), with margin for the time the test takes
        rover.recordWaveplate(new WaveplateSnapshot(240, 0, Instant.now().minus(Duration.ofMinutes(123))));
        userRepository.saveAndFlush(rover);

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(240))
                .andExpect(jsonPath("$.data.crystals").value(10));
    }

    @Test
    void overwritesThePreviousReading() throws Exception {
        mockMvc.perform(as(rover, putWith("{\"value\":50}"))).andExpect(status().isOk());
        mockMvc.perform(as(rover, putWith("{\"value\":200}"))).andExpect(status().isOk());

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data.value").value(200));
    }

    @Test
    void keepsTheWaveplateOfEachUserApart() throws Exception {
        mockMvc.perform(as(rover, putWith("{\"value\":50}"))).andExpect(status().isOk());

        mockMvc.perform(as(other, get(URL))).andExpect(status().isNotFound());
    }

    @Test
    void refusesValuesThatAreNotPossibleInTheGameAndSavesNothing() throws Exception {
        mockMvc.perform(as(rover, putWith("{\"value\":100}"))).andExpect(status().isOk());
        // Written to the database before the cache is emptied, or the first reading would be lost with it
        entityManager.flush();
        entityManager.clear();

        String[][] cases = {
            {"{\"value\":241}", "value"},
            {"{\"value\":-1}", "value"},
            {"{}", "value"},
            {"{\"value\":null}", "value"},
            {"{\"value\":240,\"crystals\":481}", "crystals"},
            {"{\"value\":100,\"crystals\":481}", "crystals"},
            {"{\"value\":240,\"crystals\":-1}", "crystals"},
        };
        for (String[] testCase : cases) {
            mockMvc.perform(as(rover, putWith(testCase[0])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields." + testCase[1]).isNotEmpty());
        }

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data.value").value(100));
    }

    @Test
    void refusesAValueThatIsNotANumber() throws Exception {
        mockMvc.perform(as(rover, putWith("{\"value\":\"many\"}"))).andExpect(status().isBadRequest());
        // A whole number written with a decimal point is refused too, not cut (spring.jackson.deserialization)
        mockMvc.perform(as(rover, putWith("{\"value\":12.5}"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, putWith("{\"value\":100.0}"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, putWith("{\"value\":100,\"crystals\":12.5}"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, putWith("{\"value\":99999999999}"))).andExpect(status().isBadRequest());
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {get(URL),
                putWith("{\"value\":10}")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }

    private static MockHttpServletRequestBuilder putWith(String json) {
        return put(URL).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
