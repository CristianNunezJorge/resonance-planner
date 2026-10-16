package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The proposals API (D22, D64, D124) through the real filter chain and the Docker Compose test database; every test is
// rolled back. Today is a fixed Monday, and every proposal made by the tests is of that day; the demo events are
// removed, so the plans do not depend on the day the tests run.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProposalControllerTest {

    private static final String URL = "/api/me/proposals";
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 14);
    private static final String SUNDAY = "2030-01-13";
    private static final String TUESDAY = "2030-01-15";

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

    @MockitoBean
    private Today today;

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsersAndRemoveTheDemoEvents() {
        when(today.date()).thenReturn(MONDAY);
        when(today.dateOf(any())).thenReturn(MONDAY);
        User withPlayTime = User.create("rover", "rover@example.com", "hash", Role.USER);
        withPlayTime.changePlayTimeMinutes(60);
        rover = userRepository.saveAndFlush(withPlayTime);
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
        jdbc.update("delete from game_event");
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get(URL + "/pending")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL + "/basic")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL + "/1/confirm")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL + "/1/reject")).andExpect(status().isUnauthorized());
    }

    @Nested
    class BasicPlan {

        @Test
        void waitsForTheUserAsAPendingProposal() throws Exception {
            addJinhsi(rover);

            long id = createBasic(rover);

            mockMvc.perform(as(rover, get(URL + "/pending")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.summary").value(startsWith("Piano base di 7 giorni, ")))
                    .andExpect(jsonPath("$.data.createdAt").isString())
                    .andExpect(jsonPath("$.data.tasks[*].status").value(everyItem(is("PROPOSED"))))
                    .andExpect(jsonPath("$.data.tasks[*].origin").value(everyItem(is("AGENT"))))
                    .andExpect(jsonPath("$.data.tasks[0].taskDate").value(MONDAY.toString()))
                    .andExpect(jsonPath("$.data.tasks[0].kind").value("FARMING"))
                    .andExpect(jsonPath("$.data.tasks[0].farmingSourceId").value("boss-mourning-aix"))
                    .andExpect(jsonPath("$.data.tasks[0].title").value("Boss Challenge: Mourning Aix"))
                    .andExpect(jsonPath("$.data.tasks[0].runs").value(2))
                    .andExpect(jsonPath("$.data.tasks[0].waveplates").value(120))
                    .andExpect(jsonPath("$.data.tasks[0].characterId").value("jinhsi"));
        }

        @Test
        void isShownApartInTheCalendarWithAllItsTasks() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);
            int tasks = proposedTasks(id);

            // Only Monday is asked, but the bar needs the whole proposal (D124, point 6)
            mockMvc.perform(as(rover, calendar(MONDAY.toString(), MONDAY.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tasks").isEmpty())
                    .andExpect(jsonPath("$.data.pendingProposal.id").value(id))
                    .andExpect(jsonPath("$.data.pendingProposal.tasks.length()").value(tasks));
            assertThat(tasks).isGreaterThan(1);
        }

        @Test
        void savesTheEventActivitiesAndPutsTheWarningsInTheSummary() throws Exception {
            // A daily activity of an hour and an hour of play: no time is left for the farming of Jinhsi
            addJinhsi(rover);
            GameEvent festival = GameEvent.custom("Festa", EventType.POINTS_SHOP, "Solo per i test.");
            festival.scheduleFixed(MONDAY, MONDAY.plusDays(30));
            festival.addActivity("Obiettivi", 60, ActivityFrequency.DAILY, null);
            Long eventId = gameEventRepository.saveAndFlush(festival).getId();

            createBasic(rover);

            mockMvc.perform(as(rover, get(URL + "/pending")))
                    .andExpect(jsonPath("$.data.summary").value(startsWith("Piano base di 7 giorni, 7 attività. ")))
                    .andExpect(jsonPath("$.data.summary").value(
                            containsString("Il piano non copre tutto il fabbisogno")))
                    .andExpect(jsonPath("$.data.tasks[*].kind").value(everyItem(is("EVENT"))))
                    .andExpect(jsonPath("$.data.tasks[0].title").value("Festa: Obiettivi"))
                    .andExpect(jsonPath("$.data.tasks[0].eventId").value(eventId))
                    .andExpect(jsonPath("$.data.tasks[0].minutes").value(60));
        }

        @Test
        void isRefusedWhenThereIsNothingToPlan() throws Exception {
            mockMvc.perform(as(rover, post(URL + "/basic")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.error.message").value(
                            "Il roster è vuoto: aggiungi un personaggio con un obiettivo per pianificare il farming."));
            assertThat(proposalsOf(rover)).isZero();
        }

        @Test
        void replacesThePendingProposalAndDeletesItsTasks() throws Exception {
            addJinhsi(rover);
            long first = createBasic(rover);

            long second = createBasic(rover);

            assertThat(statusOf(first)).isEqualTo("REPLACED");
            assertThat(decidedAt(first)).isNotNull();
            assertThat(proposedTasks(first)).isZero();
            assertThat(statusOf(second)).isEqualTo("PENDING");
            assertThat(proposedTasks(second)).isPositive();
        }
    }

    @Test
    void hasNoPendingProposalAtTheStart() throws Exception {
        mockMvc.perform(as(rover, get(URL + "/pending")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));
        mockMvc.perform(as(rover, calendar(MONDAY.toString(), MONDAY.toString())))
                .andExpect(jsonPath("$.data.pendingProposal").value(nullValue()));
    }

    @Nested
    class Confirm {

        @Test
        void putsTheTasksInTheCalendar() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);
            int tasks = proposedTasks(id);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.tasks.length()").value(tasks))
                    .andExpect(jsonPath("$.data.tasks[*].status").value(everyItem(is("CONFIRMED"))));

            assertThat(decidedAt(id)).isNotNull();
            mockMvc.perform(as(rover, calendar(MONDAY.toString(), MONDAY.plusDays(6).toString())))
                    .andExpect(jsonPath("$.data.tasks.length()").value(tasks))
                    .andExpect(jsonPath("$.data.pendingProposal").value(nullValue()));
            mockMvc.perform(as(rover, get(URL + "/pending"))).andExpect(jsonPath("$.data").value(nullValue()));
        }

        @Test
        void replacesOnlyTheConfirmedTasksOfTheAgentFromToday() throws Exception {
            addJinhsi(rover);
            long past = agentTask(rover, "CONFIRMED", SUNDAY);
            long today = agentTask(rover, "CONFIRMED", MONDAY.toString());
            long tomorrow = agentTask(rover, "CONFIRMED", TUESDAY);
            long done = agentTask(rover, "DONE", MONDAY.toString());
            long manual = manualTask(rover, TUESDAY);
            long othersTask = agentTask(other, "CONFIRMED", TUESDAY);
            long id = createBasic(rover);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm"))).andExpect(status().isOk());

            assertThat(taskStatus(today)).isEqualTo("REPLACED");
            assertThat(taskStatus(tomorrow)).isEqualTo("REPLACED");
            assertThat(taskStatus(past)).isEqualTo("CONFIRMED");
            assertThat(taskStatus(done)).isEqualTo("DONE");
            assertThat(taskStatus(manual)).isEqualTo("CONFIRMED");
            assertThat(taskStatus(othersTask)).isEqualTo("CONFIRMED");
        }

        @Test
        void worksOnlyOnce() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);
            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm"))).andExpect(status().isOk());

            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value("Questa proposta non è più in attesa."));
            mockMvc.perform(as(rover, post(URL + "/" + id + "/reject"))).andExpect(status().isConflict());
        }

        @Test
        void refusesAProposalOfAnEarlierDay() throws Exception {
            Instant sundayEvening = Instant.parse("2030-01-13T20:00:00Z");
            when(today.dateOf(sundayEvening)).thenReturn(LocalDate.parse(SUNDAY));
            Long id = jdbc.queryForObject("""
                    insert into plan_proposal (user_id, status, summary, created_at)
                    values (?, 'PENDING', 'Piano di ieri', ?) returning id""",
                    Long.class, rover.getId(), Timestamp.from(sundayEvening));

            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.message").value(
                            "La proposta è di un altro giorno: generane una nuova."));
            assertThat(statusOf(id)).isEqualTo("PENDING");
        }
    }

    @Nested
    class Reject {

        @Test
        void keepsTheProposalAndDeletesItsTasks() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);

            mockMvc.perform(as(rover, post(URL + "/" + id + "/reject")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.tasks").isEmpty());

            assertThat(statusOf(id)).isEqualTo("REJECTED");
            assertThat(decidedAt(id)).isNotNull();
            assertThat(proposedTasks(id)).isZero();
            mockMvc.perform(as(rover, get(URL + "/pending"))).andExpect(jsonPath("$.data").value(nullValue()));
        }
    }

    @Nested
    class PlanOutdated {

        @Test
        void isFalseWithoutAPlanAndRightAfterOne() throws Exception {
            addJinhsi(rover);
            assertThat(planOutdated()).isFalse();

            long id = createBasic(rover);
            assertThat(planOutdated()).isFalse();

            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm"))).andExpect(status().isOk());
            assertThat(planOutdated()).isFalse();
        }

        @Test
        void becomesTrueWhenAGoalChangesAndFalseAgainWhenItComesBack() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);
            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm"))).andExpect(status().isOk());

            setTargetRank(1);
            assertThat(planOutdated()).isTrue();

            setTargetRank(2);
            assertThat(planOutdated()).isFalse();
        }

        @Test
        void becomesTrueWhenACharacterJoinsTheRoster() throws Exception {
            addJinhsi(rover);
            createBasic(rover);

            jdbc.update("""
                    insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                        target_rank)
                    values (?, 'yinlin', 1, 0, 20, 0)""", rover.getId());
            entityManager.clear();

            assertThat(planOutdated()).isTrue();
        }

        @Test
        void looksOnlyAtTheEventsOfTheDaysOfThePlan() throws Exception {
            addJinhsi(rover);
            createBasic(rover);

            // The basic plan covers Monday to Sunday: an event of the week after does not change it
            festival(MONDAY.plusDays(8));
            assertThat(planOutdated()).isFalse();

            festival(MONDAY.plusDays(3));
            assertThat(planOutdated()).isTrue();
        }

        @Test
        void isFalseForAPlanSavedBeforeTheFingerprint() throws Exception {
            addJinhsi(rover);
            Long id = jdbc.queryForObject(
                    "insert into plan_proposal (user_id, status, summary) values (?, 'CONFIRMED', 'Vecchio') returning id",
                    Long.class, rover.getId());

            assertThat(id).isPositive();
            assertThat(planOutdated()).isFalse();
        }

        @Test
        void comesBackAfterANewProposalIsRejected() throws Exception {
            addJinhsi(rover);
            long id = createBasic(rover);
            mockMvc.perform(as(rover, post(URL + "/" + id + "/confirm"))).andExpect(status().isOk());
            setTargetRank(1);
            long fresh = createBasic(rover);
            assertThat(planOutdated()).isFalse();

            // The rejected proposal is not a plan: the confirmed one, made before the change, is the last plan again
            mockMvc.perform(as(rover, post(URL + "/" + fresh + "/reject"))).andExpect(status().isOk());

            assertThat(planOutdated()).isTrue();
        }

        @Test
        void isFalseForAPlanTooLongToReadWithTheEvents() throws Exception {
            addJinhsi(rover);
            jdbc.update("insert into plan_proposal (user_id, status, summary, inputs_hash, first_day, last_day)"
                    + " values (?, 'CONFIRMED', 'Lungo', ?, ?, ?)",
                    rover.getId(), "0".repeat(64), MONDAY, MONDAY.plusDays(70));

            assertThat(planOutdated()).isFalse();
        }

        @Test
        void doesNotLookAtThePlansOfAnotherUser() throws Exception {
            addJinhsi(other);
            createBasic(other);
            jdbc.update("update owned_character set target_rank = 1 where user_id = ?", other.getId());
            entityManager.clear();

            assertThat(planOutdated()).isFalse();
        }

        private boolean planOutdated() throws Exception {
            String value = mockMvc.perform(as(rover, calendar(MONDAY.toString(), MONDAY.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.planOutdated").isBoolean())
                    .andReturn().getResponse().getContentAsString();
            return JsonPath.<Boolean>read(value, "$.data.planOutdated");
        }

        private void setTargetRank(int rank) {
            jdbc.update("update owned_character set target_rank = ? where user_id = ?", rank, rover.getId());
            // The roster was read by the earlier requests of this test transaction: read it again from the database
            entityManager.clear();
        }

        private void festival(LocalDate day) {
            GameEvent festival = GameEvent.custom("Festa " + day, EventType.POINTS_SHOP, "Solo per i test.");
            festival.scheduleFixed(day, day);
            festival.addActivity("Obiettivi", 10, ActivityFrequency.DAILY, null);
            gameEventRepository.saveAndFlush(festival);
        }
    }

    @Test
    void answers404ForTheProposalOfAnotherUser() throws Exception {
        addJinhsi(other);
        long id = createBasic(other);

        for (String action : new String[] {"confirm", "reject"}) {
            mockMvc.perform(as(rover, post(URL + "/" + id + "/" + action)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.message").value("Proposta non trovata."));
        }
        mockMvc.perform(as(rover, post(URL + "/999999999/confirm"))).andExpect(status().isNotFound());
        // An id that is not a number is a wrong request, not an error of the server
        mockMvc.perform(as(rover, post(URL + "/abc/confirm"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(rover, post(URL + "/99999999999999999999/reject"))).andExpect(status().isBadRequest());
        assertThat(statusOf(id)).isEqualTo("PENDING");
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        addJinhsi(rover);
        long id = createBasic(rover);
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {get(URL + "/pending"),
                post(URL + "/basic"), post(URL + "/" + id + "/confirm"), post(URL + "/" + id + "/reject")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private long createBasic(User user) throws Exception {
        String json = mockMvc.perform(as(user, post(URL + "/basic")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*?\"id\":(\\d+).*", "$1"));
    }

    private void addJinhsi(User user) {
        jdbc.update("""
                insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                    target_rank)
                values (?, 'jinhsi', 1, 0, 40, 2)""", user.getId());
    }

    // A task of an earlier plan, already confirmed or done: written with SQL inside its own confirmed proposal
    private long agentTask(User user, String taskStatus, String day) {
        Long proposalId = jdbc.queryForObject(
                "insert into plan_proposal (user_id, status) values (?, 'CONFIRMED') returning id", Long.class,
                user.getId());
        return jdbc.queryForObject("""
                insert into planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes)
                values (?, ?, 'AGENT', 'CUSTOM', ?, ?::date, 'Piano vecchio', 10) returning id""",
                Long.class, user.getId(), proposalId, taskStatus, day);
    }

    private long manualTask(User user, String day) {
        return jdbc.queryForObject("""
                insert into planned_task (user_id, origin, kind, status, task_date, title, minutes)
                values (?, 'MANUAL', 'CUSTOM', 'CONFIRMED', ?::date, 'Manuale', 10) returning id""",
                Long.class, user.getId(), day);
    }

    private String statusOf(long proposalId) {
        return jdbc.queryForObject("select status from plan_proposal where id = ?", String.class, proposalId);
    }

    private Instant decidedAt(long proposalId) {
        Timestamp decided = jdbc.queryForObject("select decided_at from plan_proposal where id = ?",
                Timestamp.class, proposalId);
        return decided == null ? null : decided.toInstant();
    }

    private int proposedTasks(long proposalId) {
        return jdbc.queryForObject("select count(*) from planned_task where proposal_id = ?", Integer.class,
                proposalId);
    }

    private int proposalsOf(User user) {
        return jdbc.queryForObject("select count(*) from plan_proposal where user_id = ?", Integer.class,
                user.getId());
    }

    private String taskStatus(long taskId) {
        return jdbc.queryForObject("select status from planned_task where id = ?", String.class, taskId);
    }

    private MockHttpServletRequestBuilder calendar(String from, String to) {
        return get("/api/me/calendar").param("from", from).param("to", to);
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }
}
