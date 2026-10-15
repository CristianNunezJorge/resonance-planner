package com.resonanceplanner.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

// The admin events API (D14, D114) through the real filter chain; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminEventControllerTest {

    private static final String URL = "/api/admin/events";
    // Sat 17 and Sun 18 October are the only active days of 12-18 October
    private static final String WEEKEND_TOURNAMENT = """
            {"name":"Torneo del weekend","type":"COMBAT_STAGES","description":"Solo sabato e domenica.",
             "schedule":{"type":"FIXED","startDate":"2026-10-12","endDate":"2026-10-18"},
             "activeWeekdays":["SUNDAY","SATURDAY"],
             "rewards":["Astrite"],
             "activities":[{"title":"Livello 1","minutes":20,"frequency":"ONCE","unlockDay":6}]}""";
    private static final String DOUBLE_BOSS = """
            {"name":"Boss doppi","type":"DOUBLE_DROP","description":"Boss Challenge doppie.",
             "schedule":{"type":"RECURRING","startOffsetDays":0,"durationDays":3,"repeatEveryDays":10},
             "effect":{"type":"DROP_MULTIPLIER","sourceType":"BOSS_CHALLENGE","multiplier":1.5,"dailyLimit":2}}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private User admin;
    private User player;

    @BeforeEach
    void createAnAdminAndAPlayer() {
        admin = userRepository.saveAndFlush(User.create("boss", "boss@example.com", "hash", Role.ADMIN));
        player = userRepository.saveAndFlush(User.create("player", "player@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLoginAndTheAdminRole() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        for (MockHttpServletRequestBuilder request : everyRequest(WEEKEND_TOURNAMENT)) {
            mockMvc.perform(as(player, request)).andExpect(status().isForbidden());
        }
    }

    @Test
    void listsTheDemoEventsWithTheirStoredSchedule() throws Exception {
        mockMvc.perform(as(admin, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItem("bountiful-crescendo")))
                .andExpect(jsonPath("$.data[?(@.code == 'bountiful-crescendo')].schedule.type").value(contains("RECURRING")))
                .andExpect(jsonPath("$.data[?(@.code == 'bountiful-crescendo')].schedule.startOffsetDays").value(contains(3)))
                .andExpect(jsonPath("$.data[?(@.code == 'bountiful-crescendo')].schedule.repeatEveryDays").value(contains(14)));
    }

    @Test
    void createsAFixedWeekendEventThatThePublicListShowsOnlyOnItsDays() throws Exception {
        mockMvc.perform(as(admin, postWith(WEEKEND_TOURNAMENT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.code").value(nullValue()))
                .andExpect(jsonPath("$.data.schedule.type").value("FIXED"))
                .andExpect(jsonPath("$.data.schedule.startOffsetDays").value(nullValue()))
                .andExpect(jsonPath("$.data.activeWeekdays").value(contains("SATURDAY", "SUNDAY")))
                .andExpect(jsonPath("$.data.rewards").value(contains("Astrite")))
                .andExpect(jsonPath("$.data.activities[0].unlockDay").value(6))
                .andExpect(jsonPath("$.data.effect").value(nullValue()));

        mockMvc.perform(get("/api/events").param("from", "2026-10-12").param("to", "2026-10-18"))
                .andExpect(jsonPath("$.data[?(@.name == 'Torneo del weekend')].activeDates")
                        .value(contains(contains("2026-10-17", "2026-10-18"))));
    }

    @Test
    void createsARecurringDoubleDropWithItsEffect() throws Exception {
        mockMvc.perform(as(admin, postWith(DOUBLE_BOSS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.schedule.durationDays").value(3))
                .andExpect(jsonPath("$.data.activeWeekdays").value(nullValue()))
                .andExpect(jsonPath("$.data.rewards").isEmpty())
                .andExpect(jsonPath("$.data.activities").isEmpty())
                .andExpect(jsonPath("$.data.effect.sourceType").value("BOSS_CHALLENGE"))
                .andExpect(jsonPath("$.data.effect.multiplier").value(1.5))
                .andExpect(jsonPath("$.data.effect.dailyLimit").value(2));
    }

    @Test
    void storesAllSevenDaysAsEveryDay() throws Exception {
        String everyDay = DOUBLE_BOSS.replace("\"schedule\"", "\"activeWeekdays\":[\"MONDAY\",\"TUESDAY\","
                + "\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"schedule\"");

        mockMvc.perform(as(admin, postWith(everyDay)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activeWeekdays").value(nullValue()));
    }

    @Test
    void namesEveryFieldWithAWrongShape() throws Exception {
        String wrong = """
                {"name":" ","description":"x","schedule":{"type":"FIXED","startDate":"2026-10-12","endDate":"2026-10-13"},
                 "rewards":[""],
                 "activities":[{"title":"Livello","minutes":0,"frequency":"ONCE"}],
                 "effect":{"type":"DROP_MULTIPLIER","sourceType":"BOSS_CHALLENGE","multiplier":10.5}}""";
        long before = gameEventRepository.count();

        mockMvc.perform(as(admin, postWith(wrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.name").value("Inserisci il nome."))
                .andExpect(jsonPath("$.error.fields.type").value("Scegli il tipo di evento."))
                .andExpect(jsonPath("$.error.fields['rewards[0]']").value("La ricompensa non può essere vuota."))
                .andExpect(jsonPath("$.error.fields['activities[0].minutes']").value("I minuti vanno da 1 a 600."))
                .andExpect(jsonPath("$.error.fields['effect.multiplier']").value("Il moltiplicatore può essere al massimo 10."));
        assertThat(gameEventRepository.count()).isEqualTo(before);
    }

    @Test
    void refusesAnEmptyDayOfTheWeek() throws Exception {
        String nullDay = WEEKEND_TOURNAMENT.replace("[\"SUNDAY\",\"SATURDAY\"]", "[null]");

        // Jackson already refuses it while reading the body: 400 without fields, never a 500
        mockMvc.perform(as(admin, postWith(nullDay)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Richiesta non valida."));
    }

    @Test
    void namesEveryFieldThatBreaksARuleAndSavesNothing() throws Exception {
        String wrong = """
                {"name":"Negozio","type":"POINTS_SHOP","description":"x",
                 "schedule":{"type":"FIXED","startDate":"2026-10-14","endDate":"2026-10-12"},
                 "effect":{"type":"DROP_MULTIPLIER","sourceType":"BOSS_CHALLENGE","multiplier":2}}""";
        long before = gameEventRepository.count();

        mockMvc.perform(as(admin, postWith(wrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields['schedule.endDate']").value("La fine non può venire prima dell'inizio."))
                .andExpect(jsonPath("$.error.fields.effect").value("Solo gli eventi di ricompense doppie hanno un effetto."));
        assertThat(gameEventRepository.count()).isEqualTo(before);
    }

    @Test
    void replacesADemoEventAndKeepsItsCode() throws Exception {
        Long lament = gameEventRepository.findByCode("lament-recon-solaris-soldier").orElseThrow().getId();

        // The 7 drills become one activity and the event gets fixed dates
        mockMvc.perform(as(admin, put(URL + "/" + lament).contentType(MediaType.APPLICATION_JSON).content(WEEKEND_TOURNAMENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lament))
                .andExpect(jsonPath("$.data.code").value("lament-recon-solaris-soldier"))
                .andExpect(jsonPath("$.data.name").value("Torneo del weekend"))
                .andExpect(jsonPath("$.data.schedule.type").value("FIXED"))
                .andExpect(jsonPath("$.data.schedule.durationDays").value(nullValue()))
                .andExpect(jsonPath("$.data.activities", hasSize(1)));
        assertThat(jdbc.queryForObject("select count(*) from event_activity where event_id = ?", Integer.class, lament))
                .isEqualTo(1);
    }

    @Test
    void answersNotFoundForAnEventThatDoesNotExist() throws Exception {
        mockMvc.perform(as(admin, put(URL + "/999999").contentType(MediaType.APPLICATION_JSON).content(WEEKEND_TOURNAMENT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Evento non trovato."));
        mockMvc.perform(as(admin, delete(URL + "/999999"))).andExpect(status().isNotFound());
    }

    @Test
    void deletesAnEventWithItsActivitiesOnce() throws Exception {
        String created = mockMvc.perform(as(admin, postWith(WEEKEND_TOURNAMENT)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.data.id");
        // An activity of the calendar planned for this event (D35)
        Long proposal = jdbc.queryForObject(
                "insert into plan_proposal (user_id, status) values (?, 'CONFIRMED') returning id", Long.class,
                admin.getId());
        Long task = jdbc.queryForObject("insert into planned_task (user_id, proposal_id, origin, kind, status, "
                + "task_date, title, minutes, event_id) values (?, ?, 'AGENT', 'EVENT', 'CONFIRMED', '2026-10-17', "
                + "'Livello 1', 20, ?) returning id", Long.class, admin.getId(), proposal, id.longValue());

        mockMvc.perform(as(admin, delete(URL + "/" + id))).andExpect(status().isNoContent());

        // The activity of the calendar stays, without the link to the event (D114)
        assertThat(jdbc.queryForObject("select event_id from planned_task where id = ?", Long.class, task)).isNull();
        for (String table : new String[] {"event_activity", "event_active_weekday", "event_reward"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where event_id = ?", Integer.class,
                    id.longValue())).as(table).isZero();
        }

        mockMvc.perform(get("/api/events/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(as(admin, get(URL))).andExpect(jsonPath("$.data[*].id").value(not(hasItem(id.intValue()))));
        mockMvc.perform(as(admin, delete(URL + "/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void doesNotLetADeletedAdminBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(admin.getId(), admin.getRole());
        jdbc.update("delete from users where id = ?", admin.getId());
        // Inside the transaction of the test the cached user would hide the change made with SQL
        entityManager.clear();

        // The token is valid for 24 hours (D45), but its user is gone: 401 (D102)
        for (MockHttpServletRequestBuilder request : everyRequest(WEEKEND_TOURNAMENT)) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void refusesAnAdminTokenWhoseUserIsNotAdminAnymore() throws Exception {
        String token = jwtService.generateToken(admin.getId(), admin.getRole());
        jdbc.update("update users set role = 'USER' where id = ?", admin.getId());
        entityManager.clear();

        for (MockHttpServletRequestBuilder request : everyRequest(WEEKEND_TOURNAMENT)) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    private MockHttpServletRequestBuilder[] everyRequest(String body) {
        Long bountiful = gameEventRepository.findByCode("bountiful-crescendo").orElseThrow().getId();
        return new MockHttpServletRequestBuilder[] {get(URL), postWith(body),
                put(URL + "/" + bountiful).contentType(MediaType.APPLICATION_JSON).content(body),
                delete(URL + "/" + bountiful)};
    }

    private static MockHttpServletRequestBuilder postWith(String json) {
        return post(URL).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }
}
