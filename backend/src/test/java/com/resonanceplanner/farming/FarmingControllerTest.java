package com.resonanceplanner.farming;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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

// The farming API (D112) with the real catalog loaded from the JSON files; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FarmingControllerTest {

    private static final String PLAN = "/api/me/farming-plan";
    private static final String JINHSI_REQUIREMENTS = "/api/me/roster/jinhsi/requirements";
    private static final String JINHSI = "{\"characterId\":\"jinhsi\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";
    private static final String YINLIN = "{\"characterId\":\"yinlin\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";
    private static final String CHANGLI = "{\"characterId\":\"changli\",\"currentLevel\":1,\"currentRank\":0,\"targetLevel\":40,\"targetRank\":2}";

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
        User withPlayTime = User.create("rover", "rover@example.com", "hash", Role.USER);
        withPlayTime.changePlayTimeMinutes(60);
        rover = userRepository.saveAndFlush(withPlayTime);
        // The other user has not chosen a play time yet
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get(PLAN)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(JINHSI_REQUIREMENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void needsNothingWithAnEmptyRoster() throws Exception {
        mockMvc.perform(as(rover, get(PLAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materials").isEmpty())
                .andExpect(jsonPath("$.data.sources").isEmpty())
                .andExpect(jsonPath("$.data.totals.waveplates").value(0))
                .andExpect(jsonPath("$.data.totals.minutes").value(0))
                .andExpect(jsonPath("$.data.totals.estimatedDays").value(0))
                .andExpect(jsonPath("$.data.containsEstimates").value(false));
    }

    @Test
    void plansJinhsiFromTheStartToTheCapWithTheRealCatalog() throws Exception {
        addToRoster(rover, JINHSI);

        // The example of D112: the real EXP table gives 208,800 EXP from level 1 to 40
        mockMvc.perform(as(rover, get(PLAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials.length()").value(6))
                .andExpect(jsonPath("$.data.materials[0].materialId").value("elegy-tacet-core"))
                .andExpect(jsonPath("$.data.materials[0].name").value("Elegy Tacet Core"))
                .andExpect(jsonPath("$.data.materials[0].category").value("BOSS_DROP"))
                .andExpect(jsonPath("$.data.materials[0].required").value(3))
                .andExpect(jsonPath("$.data.materials[4].materialId").value("character-exp"))
                .andExpect(jsonPath("$.data.materials[4].required").value(208_800))
                .andExpect(jsonPath("$.data.materials[5].materialId").value("shell-credit"))
                .andExpect(jsonPath("$.data.materials[5].required").value(88_080))
                .andExpect(jsonPath("$.data.materials[5].missing").value(88_080))
                .andExpect(jsonPath("$.data.sources[*].farmingSourceId").value(contains(
                        "boss-mourning-aix", "overworld-howlers", "collect-loongs-pearl", "sim-resonance-potion",
                        "sim-shell-credit")))
                .andExpect(jsonPath("$.data.sources[*].runs").value(contains(2, 2, 1, 3, 1)))
                .andExpect(jsonPath("$.data.sources[0].name").value("Boss Challenge: Mourning Aix"))
                .andExpect(jsonPath("$.data.sources[0].waveplates").value(120))
                .andExpect(jsonPath("$.data.sources[0].minutes").value(8))
                .andExpect(jsonPath("$.data.totals.waveplates").value(280))
                .andExpect(jsonPath("$.data.totals.minutes").value(50))
                .andExpect(jsonPath("$.data.totals.estimatedDays").value(2))
                .andExpect(jsonPath("$.data.containsEstimates").value(true));
    }

    @Test
    void sumsTheRosterCountsTheInventoryOnceAndLeavesOutTheOtherUsers() throws Exception {
        addToRoster(rover, JINHSI);
        addToRoster(rover, YINLIN);
        setInventory(rover, "premium-resonance-potion", 5);
        addToRoster(other, CHANGLI);
        setInventory(other, "premium-resonance-potion", 900);

        mockMvc.perform(as(rover, get(PLAN)))
                .andExpect(status().isOk())
                // 2 × 208,800 EXP; 5 potions of 20,000 EXP count once for both characters
                .andExpect(jsonPath("$.data.materials[?(@.materialId == 'character-exp')].required").value(hasItem(417_600)))
                .andExpect(jsonPath("$.data.materials[?(@.materialId == 'character-exp')].owned").value(hasItem(100_000)))
                .andExpect(jsonPath("$.data.materials[*].materialId").value(hasItem("group-abomination-tacet-core")))
                // Changli is in the roster of the other user: its boss material is not here
                .andExpect(jsonPath("$.data.materials[*].materialId").value(not(hasItem("rage-tacet-core"))));
    }

    @Test
    void showsWhatOneCharacterNeedsWithTheWholeInventoryAndItsCompletion() throws Exception {
        addToRoster(rover, JINHSI);
        addToRoster(rover, YINLIN);
        setInventory(rover, "elegy-tacet-core", 3);
        setInventory(rover, "lf-howler-core", 10);

        // 6 materials: the boss core and the LF core are all owned, the other four are not: 200 / 6 = 33
        mockMvc.perform(as(rover, get(JINHSI_REQUIREMENTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterId").value("jinhsi"))
                .andExpect(jsonPath("$.data.completionPercent").value(33))
                .andExpect(jsonPath("$.data.materials.length()").value(6))
                .andExpect(jsonPath("$.data.materials[?(@.materialId == 'lf-howler-core')].missing").value(hasItem(0)))
                .andExpect(jsonPath("$.data.materials[*].materialId").value(not(hasItem("lf-whisperin-core"))))
                .andExpect(jsonPath("$.data.sources[*].farmingSourceId").value(not(hasItem("boss-mourning-aix"))))
                .andExpect(jsonPath("$.data.totals.estimatedDays").isNumber());
    }

    @Test
    void answersNotFoundForACharacterThatIsNotInTheRoster() throws Exception {
        addToRoster(other, JINHSI);

        // Not in the roster, in the roster of another user, not in the catalog: always the same 404
        for (String characterId : new String[] {"jinhsi", "yinlin", "nobody"}) {
            mockMvc.perform(as(rover, get("/api/me/roster/" + characterId + "/requirements")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.message").value("Questo personaggio non è nel tuo roster."));
        }
    }

    @Test
    void hasNoEstimatedDaysBeforeThePlayTimeIsChosen() throws Exception {
        addToRoster(other, JINHSI);

        mockMvc.perform(as(other, get(PLAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totals.waveplates").value(280))
                .andExpect(jsonPath("$.data.totals.estimatedDays").value(nullValue()));
        mockMvc.perform(as(other, get(JINHSI_REQUIREMENTS)))
                .andExpect(jsonPath("$.data.totals.estimatedDays").value(nullValue()));
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        addToRoster(rover, JINHSI);
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 (D102)
        for (String url : new String[] {PLAN, JINHSI_REQUIREMENTS}) {
            mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    private void addToRoster(User user, String json) throws Exception {
        mockMvc.perform(as(user, post("/api/me/roster").contentType(MediaType.APPLICATION_JSON).content(json)))
                .andExpect(status().isCreated());
    }

    private void setInventory(User user, String materialId, int quantity) throws Exception {
        mockMvc.perform(as(user, put("/api/me/inventory/" + materialId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + quantity + "}")))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }
}
