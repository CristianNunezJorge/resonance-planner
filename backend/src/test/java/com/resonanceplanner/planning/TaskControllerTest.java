package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

// The tasks API through the real filter chain and the Docker Compose test database; every test is rolled back.
// The demo events are removed at the start of each test (and come back with the rollback): every event effect here is
// written by the test itself, so the multipliers do not depend on the day the tests run.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerTest {

    private static final String URL = "/api/me/tasks";
    private static final String MONDAY = "2030-01-14";
    private static final String TUESDAY = "2030-01-15";
    private static final String SIM_POTIONS = "sim-resonance-potion";
    private static final String BOSS = "boss-mourning-aix";
    private static final String VALID_BODY = """
            {"title":"Giro dei boss","taskDate":"2030-01-14","minutes":30,"notes":"Prima Mourning Aix"}""";

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

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsersAndRemoveTheDemoEvents() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
        jdbc.update("delete from game_event");
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(postWith(URL, VALID_BODY)).andExpect(status().isUnauthorized());
        mockMvc.perform(putWith(URL + "/1", VALID_BODY)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(URL + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL + "/1/complete")).andExpect(status().isUnauthorized());
    }

    @Nested
    class ManualTasks {

        @Test
        void areCreatedConfirmedAndShownInTheCalendar() throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"  Giro dei boss  ","taskDate":"2030-01-14","minutes":30,"notes":"Prima Mourning Aix"}""")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.origin").value("MANUAL"))
                    .andExpect(jsonPath("$.data.kind").value("CUSTOM"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.taskDate").value(MONDAY))
                    .andExpect(jsonPath("$.data.title").value("Giro dei boss"))
                    .andExpect(jsonPath("$.data.notes").value("Prima Mourning Aix"))
                    .andExpect(jsonPath("$.data.minutes").value(30))
                    .andExpect(jsonPath("$.data.waveplates").value(0))
                    .andExpect(jsonPath("$.data.runs").value(nullValue()))
                    .andExpect(jsonPath("$.data.farmingSourceId").value(nullValue()))
                    .andExpect(jsonPath("$.data.completedAt").value(nullValue()));

            mockMvc.perform(as(rover, calendar(MONDAY, MONDAY)))
                    .andExpect(jsonPath("$.data.tasks.length()").value(1))
                    .andExpect(jsonPath("$.data.tasks[0].title").value("Giro dei boss"));
        }

        @Test
        void keepNoNotesWhenTheyAreEmpty() throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"Raccolta","taskDate":"2030-01-14","minutes":10,"notes":"   "}""")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.notes").value(nullValue()));
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"Raccolta","taskDate":"2030-01-14","minutes":10}""")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.notes").value(nullValue()));
        }

        @Test
        void ignoreOriginStatusAndUserSentInTheBody() throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"Furba","taskDate":"2030-01-14","minutes":10,"origin":"AGENT","kind":"FARMING",
                             "status":"DONE","userId":%d}""".formatted(other.getId()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.origin").value("MANUAL"))
                    .andExpect(jsonPath("$.data.kind").value("CUSTOM"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

            assertThat(tasksOf(other)).isZero();
            assertThat(tasksOf(rover)).isEqualTo(1);
        }

        @Test
        void refuseEveryWrongFieldTogetherAndSaveNothing() throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"   ","minutes":0,"notes":"%s"}""".formatted("n".repeat(1001)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields.title").value("Scrivi un titolo."))
                    .andExpect(jsonPath("$.error.fields.taskDate").value("Scegli il giorno."))
                    .andExpect(jsonPath("$.error.fields.minutes").value("La durata deve essere di almeno 1 minuto."))
                    .andExpect(jsonPath("$.error.fields.notes").value("Le note possono avere al massimo 1000 caratteri."));

            assertThat(tasksOf(rover)).isZero();
        }

        @Test
        void acceptTheLimitsOfTitleDurationAndNotes() throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"%s","taskDate":"2030-01-14","minutes":1440,"notes":"%s"}"""
                            .formatted("t".repeat(120), "n".repeat(1000)))))
                    .andExpect(status().isCreated());
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"%s","taskDate":"2030-01-14","minutes":1441}""".formatted("t".repeat(121)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.title").value("Il titolo può avere al massimo 120 caratteri."))
                    .andExpect(jsonPath("$.error.fields.minutes").value("La durata non può superare 1440 minuti, un giorno intero."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1999-12-31", "2101-01-01", "+999999999-12-31"})
        void refuseADayOutsideTheWindowOfTheApp(String day) throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"Lontano","taskDate":"%s","minutes":10}""".formatted(day))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.taskDate").value("Le date vanno dal 01/01/2000 al 31/12/2100."));
            assertThat(tasksOf(rover)).isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {"14/01/2030", "2030-02-30", "domani"})
        void refuseADayThatIsNotADate(String day) throws Exception {
            mockMvc.perform(as(rover, postWith(URL, """
                            {"title":"Data strana","taskDate":"%s","minutes":10}""".formatted(day))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        void areChangedWithEveryField() throws Exception {
            long id = createManual(rover);

            mockMvc.perform(as(rover, putWith(URL + "/" + id, """
                            {"title":"Nuovo titolo","taskDate":"2030-01-15","minutes":45,"notes":""}""")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.title").value("Nuovo titolo"))
                    .andExpect(jsonPath("$.data.taskDate").value(TUESDAY))
                    .andExpect(jsonPath("$.data.minutes").value(45))
                    .andExpect(jsonPath("$.data.notes").value(nullValue()))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

            mockMvc.perform(as(rover, calendar(MONDAY, TUESDAY)))
                    .andExpect(jsonPath("$.data.tasks.length()").value(1))
                    .andExpect(jsonPath("$.data.tasks[0].taskDate").value(TUESDAY));
        }

        @Test
        void refuseAWrongChangeAndKeepTheOldValues() throws Exception {
            long id = createManual(rover);

            mockMvc.perform(as(rover, putWith(URL + "/" + id, """
                            {"title":"","taskDate":"2030-01-15","minutes":45}""")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.title").exists());
            mockMvc.perform(as(rover, putWith(URL + "/" + id, """
                            {"title":"Ok","taskDate":"2101-01-01","minutes":45}""")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.taskDate").exists());

            mockMvc.perform(as(rover, calendar(MONDAY, MONDAY)))
                    .andExpect(jsonPath("$.data.tasks[0].title").value("Giro dei boss"))
                    .andExpect(jsonPath("$.data.tasks[0].minutes").value(30));
        }

        @Test
        void areDeleted() throws Exception {
            long id = createManual(rover);

            mockMvc.perform(as(rover, delete(URL + "/" + id))).andExpect(status().isNoContent());
            mockMvc.perform(as(rover, delete(URL + "/" + id))).andExpect(status().isNotFound());
            assertThat(tasksOf(rover)).isZero();
        }

        @Test
        void canBeChangedAndDeletedAlsoWhenDone() throws Exception {
            long id = createManual(rover);
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete"))).andExpect(status().isOk());

            mockMvc.perform(as(rover, putWith(URL + "/" + id, VALID_BODY)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DONE"))
                    .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
            mockMvc.perform(as(rover, delete(URL + "/" + id))).andExpect(status().isNoContent());
        }

        @Test
        void areAtMostFiveHundredForEachUser() throws Exception {
            jdbc.update("""
                    insert into planned_task (user_id, origin, kind, status, task_date, title, minutes)
                    select ?, 'MANUAL', 'CUSTOM', 'CONFIRMED', date '2030-01-01', 'Vecchia', 10
                    from generate_series(1, 499)""", rover.getId());
            // The tasks of the agent do not count
            farmingTask(rover, MONDAY, SIM_POTIONS, 1);

            mockMvc.perform(as(rover, postWith(URL, VALID_BODY))).andExpect(status().isCreated());
            mockMvc.perform(as(rover, postWith(URL, VALID_BODY)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.error.message")
                            .value("Hai già 500 attività: cancellane qualcuna prima di crearne altre."));
            // The limit is per user
            mockMvc.perform(as(other, postWith(URL, VALID_BODY))).andExpect(status().isCreated());
        }
    }

    @Nested
    class TasksOfOthers {

        @Test
        void answerNotFoundForTheTaskOfAnotherUser() throws Exception {
            long id = createManual(other);

            mockMvc.perform(as(rover, putWith(URL + "/" + id, VALID_BODY)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value("Attività non trovata."));
            mockMvc.perform(as(rover, delete(URL + "/" + id))).andExpect(status().isNotFound());
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete"))).andExpect(status().isNotFound());

            mockMvc.perform(as(other, calendar(MONDAY, MONDAY)))
                    .andExpect(jsonPath("$.data.tasks[0].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.tasks[0].title").value("Giro dei boss"));
        }

        @Test
        void answerNotFoundForATaskThatDoesNotExist() throws Exception {
            mockMvc.perform(as(rover, putWith(URL + "/999999999", VALID_BODY))).andExpect(status().isNotFound());
            mockMvc.perform(as(rover, post(URL + "/999999999/complete"))).andExpect(status().isNotFound());
        }

        @Test
        void answerBadRequestForAnIdThatIsNotANumber() throws Exception {
            mockMvc.perform(as(rover, delete(URL + "/abc"))).andExpect(status().isBadRequest());
            mockMvc.perform(as(rover, post(URL + "/abc/complete"))).andExpect(status().isBadRequest());
        }

        @Test
        void cannotBeChangedOrDeletedWhenTheyComeFromTheAgent() throws Exception {
            long id = farmingTask(rover, MONDAY, SIM_POTIONS, 3);

            mockMvc.perform(as(rover, putWith(URL + "/" + id, VALID_BODY)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message")
                            .value("Puoi modificare e cancellare solo le attività che hai creato tu."));
            mockMvc.perform(as(rover, delete(URL + "/" + id))).andExpect(status().isConflict());

            mockMvc.perform(as(rover, calendar(MONDAY, MONDAY)))
                    .andExpect(jsonPath("$.data.tasks[0].origin").value("AGENT"))
                    .andExpect(jsonPath("$.data.tasks[0].runs").value(3));
        }
    }

    @Nested
    class Completion {

        @Test
        void marksAManualTaskAsDoneWithoutTouchingTheInventory() throws Exception {
            long id = createManual(rover);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.task.id").value(id))
                    .andExpect(jsonPath("$.data.task.status").value("DONE"))
                    .andExpect(jsonPath("$.data.task.completedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.addedToInventory").isEmpty());

            assertThat(inventoryOf(rover)).isEmpty();
            mockMvc.perform(as(rover, calendar(MONDAY, MONDAY)))
                    .andExpect(jsonPath("$.data.tasks[0].status").value("DONE"));
        }

        @Test
        void refusesATaskAlreadyDone() throws Exception {
            long id = farmingTask(rover, MONDAY, BOSS, 1);
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete"))).andExpect(status().isOk());

            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value("Hai già segnato come fatta questa attività."));
            // The drops were added only once
            assertThat(inventoryOf(rover)).containsEntry("elegy-tacet-core", 2);
        }

        @ParameterizedTest
        @ValueSource(strings = {"PROPOSED", "REJECTED", "REPLACED"})
        void refusesATaskThatIsNotConfirmed(String taskStatus) throws Exception {
            long id = agentTask(rover, "FARMING", taskStatus, MONDAY, BOSS, 1);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value("Puoi segnare come fatta solo un'attività del calendario."));
            assertThat(inventoryOf(rover)).isEmpty();
        }

        @Test
        void addsTheAverageDropsOfAFarmingTaskWithoutAnEvent() throws Exception {
            jdbc.update("insert into inventory_item (user_id, material_id, quantity) values (?, 'shell-credit', 100)",
                    rover.getId());
            long id = farmingTask(rover, MONDAY, SIM_POTIONS, 3);

            // 3 runs: 3.30 premium, 15.12 advanced, 7.05 medium and 10800 credits, rounded down
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.task.status").value("DONE"))
                    .andExpect(jsonPath("$.data.task.waveplates").value(120))
                    .andExpect(jsonPath("$.data.addedToInventory.length()").value(4))
                    .andExpect(jsonPath("$.data.addedToInventory[0].materialId").value("premium-resonance-potion"))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(3))
                    .andExpect(jsonPath("$.data.addedToInventory[3].materialId").value("shell-credit"))
                    .andExpect(jsonPath("$.data.addedToInventory[3].quantity").value(10800));

            assertThat(inventoryOf(rover)).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "premium-resonance-potion", 3,
                    "advanced-resonance-potion", 15,
                    "medium-resonance-potion", 7,
                    "shell-credit", 10900));
        }

        @Test
        void multipliesOnlyTheRunsInsideTheDailyLimitOfAnActiveEvent() throws Exception {
            event(LocalDate.parse(MONDAY), LocalDate.parse(TUESDAY), Set.of(), FarmingSourceType.SIMULATION_TRAINING, "2", 3);
            long id = farmingTask(rover, MONDAY, SIM_POTIONS, 5);

            // 5 runs, the first 3 doubled: as if they were 8 runs
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete"))).andExpect(status().isOk());

            assertThat(inventoryOf(rover)).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "premium-resonance-potion", 8,      // 8.80
                    "advanced-resonance-potion", 40,    // 40.32
                    "medium-resonance-potion", 18,      // 18.80
                    "shell-credit", 28800));
        }

        @Test
        void countsTheRunsAlreadyDoneThatDayAgainstTheDailyLimit() throws Exception {
            event(LocalDate.parse(MONDAY), LocalDate.parse(MONDAY), Set.of(), FarmingSourceType.SIMULATION_TRAINING, "2", 3);
            long first = farmingTask(rover, MONDAY, "sim-shell-credit", 2);
            long second = farmingTask(rover, MONDAY, "sim-shell-credit", 2);
            // A task of another user on the same day does not use up the limit of rover
            long ofOther = farmingTask(other, MONDAY, "sim-shell-credit", 3);
            mockMvc.perform(as(other, post(URL + "/" + ofOther + "/complete"))).andExpect(status().isOk());

            // First task: both runs doubled (4 × 72000); second: 1 run doubled and 1 normal (3 × 72000)
            mockMvc.perform(as(rover, post(URL + "/" + first + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(288000));
            mockMvc.perform(as(rover, post(URL + "/" + second + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(216000));

            assertThat(inventoryOf(rover)).containsEntry("shell-credit", 504000);
        }

        @Test
        void ignoresAnEventOnAnotherDayOrOnAnotherTypeOfSource() throws Exception {
            event(LocalDate.parse(TUESDAY), LocalDate.parse(TUESDAY), Set.of(), FarmingSourceType.SIMULATION_TRAINING, "2", null);
            event(LocalDate.parse(MONDAY), LocalDate.parse(MONDAY), Set.of(), FarmingSourceType.BOSS_CHALLENGE, "2", null);
            long id = farmingTask(rover, MONDAY, "sim-shell-credit", 1);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(72000));
        }

        @Test
        void ignoresAnEventOnTheDaysOfTheWeekWhenItIsNotActive() throws Exception {
            // From Monday to Sunday, but only on Saturday (D96): the Monday task gets nothing more
            event(LocalDate.parse(MONDAY), LocalDate.parse("2030-01-20"), Set.of(DayOfWeek.SATURDAY),
                    FarmingSourceType.SIMULATION_TRAINING, "2", null);
            long monday = farmingTask(rover, MONDAY, "sim-shell-credit", 1);
            long saturday = farmingTask(rover, "2030-01-19", "sim-shell-credit", 1);

            mockMvc.perform(as(rover, post(URL + "/" + monday + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(72000));
            mockMvc.perform(as(rover, post(URL + "/" + saturday + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(144000));
        }

        @Test
        void usesTheStrongestOfTwoEventsOnTheSameDay() throws Exception {
            event(LocalDate.parse(MONDAY), LocalDate.parse(MONDAY), Set.of(), FarmingSourceType.SIMULATION_TRAINING, "2", null);
            event(LocalDate.parse(MONDAY), LocalDate.parse(MONDAY), Set.of(), FarmingSourceType.SIMULATION_TRAINING, "3", 1);
            long id = farmingTask(rover, MONDAY, "sim-shell-credit", 2);

            // The triple wins, but only for 1 run: 3 + 1 = 4 runs (the double is not added on top)
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(288000));
        }

        @Test
        void neverGoesBeyondTheHighestQuantityOfTheInventory() throws Exception {
            jdbc.update("insert into inventory_item (user_id, material_id, quantity) values (?, 'shell-credit', 999990000)",
                    rover.getId());
            long id = farmingTask(rover, MONDAY, "sim-shell-credit", 1);

            // The answer says what the run gave; the inventory stops at the highest quantity
            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.addedToInventory[0].quantity").value(72000));

            assertThat(inventoryOf(rover)).containsEntry("shell-credit", 999_999_999);
        }

        @Test
        void marksAnEventTaskAsDoneWithoutTouchingTheInventory() throws Exception {
            long id = agentTask(rover, "EVENT", "CONFIRMED", MONDAY, null, null);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/complete")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.task.kind").value("EVENT"))
                    .andExpect(jsonPath("$.data.addedToInventory").isEmpty());
            assertThat(inventoryOf(rover)).isEmpty();
        }
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        long id = createManual(rover);
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {postWith(URL, VALID_BODY),
                putWith(URL + "/" + id, VALID_BODY), delete(URL + "/" + id), post(URL + "/" + id + "/complete")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private long createManual(User user) throws Exception {
        String json = mockMvc.perform(as(user, postWith(URL, VALID_BODY)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    // Tasks of the agent exist only inside a proposal (D35): until the proposals arrive (T30) they are written with SQL
    private long agentTask(User user, String kind, String taskStatus, String day, String sourceId, Integer runs) {
        Long proposalId = jdbc.queryForObject(
                "insert into plan_proposal (user_id, status) values (?, 'CONFIRMED') returning id", Long.class, user.getId());
        return jdbc.queryForObject("""
                insert into planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes,
                    farming_source_id, runs)
                values (?, ?, 'AGENT', ?, ?, ?::date, 'Attività dell''agente', 10, ?, ?) returning id""",
                Long.class, user.getId(), proposalId, kind, taskStatus, day, sourceId, runs);
    }

    private long farmingTask(User user, String day, String sourceId, int runs) {
        return agentTask(user, "FARMING", "CONFIRMED", day, sourceId, runs);
    }

    private void event(LocalDate start, LocalDate end, Set<DayOfWeek> weekdays, FarmingSourceType type,
            String multiplier, Integer dailyLimit) {
        GameEvent event = GameEvent.custom("Ricompense doppie di prova", EventType.DOUBLE_DROP, "Solo per i test.");
        event.scheduleFixed(start, end);
        event.setActiveWeekdays(weekdays);
        event.setEffect(new EventEffect(EffectType.DROP_MULTIPLIER, type, new BigDecimal(multiplier), dailyLimit));
        gameEventRepository.saveAndFlush(event);
    }

    private Map<String, Integer> inventoryOf(User user) {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        jdbc.query("select material_id, quantity from inventory_item where user_id = ? order by material_id",
                row -> {
                    inventory.put(row.getString("material_id"), row.getInt("quantity"));
                }, user.getId());
        return inventory;
    }

    private Integer tasksOf(User user) {
        return jdbc.queryForObject("select count(*) from planned_task where user_id = ?", Integer.class, user.getId());
    }

    private MockHttpServletRequestBuilder calendar(String from, String to) {
        return get("/api/me/calendar").param("from", from).param("to", to);
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }

    private static MockHttpServletRequestBuilder postWith(String url, String json) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder putWith(String url, String json) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
