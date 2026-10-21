package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

// The calendar of the logged user (D51) on the Docker Compose test database; every test is rolled back.
// The demo events are removed at the start (they come back with the rollback), so the events of the answer are only
// those written by the test.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalendarControllerTest {

    private static final String URL = "/api/me/calendar";
    private static final String MONDAY = "2030-01-14";
    private static final String TUESDAY = "2030-01-15";
    private static final String EXPORT_URL = URL + "/export";
    // What the frontend asks for (D133): the file, or the JSON of an error
    private static final String EXPORT_ACCEPT = PlanSpreadsheet.CONTENT_TYPE + ", " + MediaType.APPLICATION_JSON_VALUE;

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
        mockMvc.perform(get(URL).param("from", MONDAY).param("to", TUESDAY)).andExpect(status().isUnauthorized());
    }

    @Test
    void bringsTheEventsAndTheTasksOfTheRangeInOneAnswer() throws Exception {
        // Active from Monday to Sunday but only on Tuesday and Saturday: in the range only Tuesday (D96)
        GameEvent event = GameEvent.custom("Torneo di prova", EventType.COMBAT_STAGES, "Solo martedì e sabato.");
        event.scheduleFixed(LocalDate.parse(MONDAY), LocalDate.parse("2030-01-20"));
        event.setActiveWeekdays(Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY));
        gameEventRepository.saveAndFlush(event);

        long manualTuesday = manualTask(rover, TUESDAY, "CONFIRMED");
        long farmingMonday = agentTask(rover, "FARMING", "CONFIRMED", MONDAY, "sim-resonance-potion", 3);
        long doneMonday = manualTask(rover, MONDAY, "DONE");
        // Not in the calendar: proposals still waiting, discarded or replaced plans, other days, other users
        agentTask(rover, "FARMING", "PROPOSED", MONDAY, "sim-resonance-potion", 1);
        agentTask(rover, "EVENT", "REJECTED", MONDAY, null, null);
        agentTask(rover, "EVENT", "REPLACED", TUESDAY, null, null);
        manualTask(rover, "2030-01-13", "CONFIRMED");
        manualTask(rover, "2030-01-16", "CONFIRMED");
        manualTask(other, MONDAY, "CONFIRMED");

        mockMvc.perform(as(rover, get(URL).param("from", MONDAY).param("to", TUESDAY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.events.length()").value(1))
                .andExpect(jsonPath("$.data.events[0].name").value("Torneo di prova"))
                .andExpect(jsonPath("$.data.events[0].activeDates").value(contains(TUESDAY)))
                // By day, then in order of creation
                .andExpect(jsonPath("$.data.tasks[*].id").value(contains(
                        (int) farmingMonday, (int) doneMonday, (int) manualTuesday)))
                .andExpect(jsonPath("$.data.tasks[0].kind").value("FARMING"))
                .andExpect(jsonPath("$.data.tasks[0].farmingSourceId").value("sim-resonance-potion"))
                .andExpect(jsonPath("$.data.tasks[0].runs").value(3))
                // 3 runs of 40 Waveplate
                .andExpect(jsonPath("$.data.tasks[0].waveplates").value(120))
                .andExpect(jsonPath("$.data.tasks[1].status").value("DONE"))
                .andExpect(jsonPath("$.data.tasks[2].origin").value("MANUAL"));
    }

    @Test
    void isEmptyWithoutEventsAndTasks() throws Exception {
        mockMvc.perform(as(rover, get(URL).param("from", MONDAY).param("to", TUESDAY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events").isEmpty())
                .andExpect(jsonPath("$.data.tasks").isEmpty());
    }

    @Test
    void checksTheRangeLikeTheEvents() throws Exception {
        mockMvc.perform(as(rover, get(URL).param("from", "2030-01-01").param("to", "2030-03-03")))
                .andExpect(status().isOk());
        mockMvc.perform(as(rover, get(URL).param("from", "2030-01-01").param("to", "2030-03-04")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.to").value("L'intervallo può coprire al massimo 62 giorni."));
        mockMvc.perform(as(rover, get(URL).param("from", TUESDAY).param("to", MONDAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.to").value("La data di fine non può venire prima di quella di inizio."));
        mockMvc.perform(as(rover, get(URL).param("from", "1999-12-31").param("to", "2000-01-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.from").value("Le date vanno dal 01/01/2000 al 31/12/2100."));
        mockMvc.perform(as(rover, get(URL).param("to", TUESDAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(as(rover, get(URL).param("from", "14/01/2030").param("to", TUESDAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL).param("from", MONDAY).param("to", TUESDAY)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void exportNeedsTheLogin() throws Exception {
        mockMvc.perform(export(MONDAY, TUESDAY)).andExpect(status().isUnauthorized());
    }

    @Test
    void exportsTheTasksAndTheEventsOfTheCalendarAsAnExcelFile() throws Exception {
        GameEvent event = GameEvent.custom("Torneo di prova", EventType.COMBAT_STAGES, "Tutti i giorni.");
        event.scheduleFixed(LocalDate.parse(MONDAY), LocalDate.parse("2030-01-20"));
        gameEventRepository.saveAndFlush(event);
        long farming = agentTask(rover, "FARMING", "CONFIRMED", MONDAY, "sim-resonance-potion", 3);
        jdbc.update("update planned_task set character_id = 'jinhsi' where id = ?", farming);
        manualTask(rover, TUESDAY, "DONE");
        // Not in the file, as not in the calendar: the pending proposal and the tasks of another user
        agentTask(rover, "FARMING", "PROPOSED", MONDAY, "sim-resonance-potion", 1);
        manualTask(other, MONDAY, "CONFIRMED");

        byte[] file = mockMvc.perform(as(rover, export(MONDAY, TUESDAY)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(PlanSpreadsheet.CONTENT_TYPE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resonance-planner_2030-01-14_2030-01-15.xlsx\""))
                // Personal data: no copy in the caches on the way
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet plan = workbook.getSheet("Piano");
            // The header, two tasks, the totals
            assertThat(plan.getLastRowNum()).isEqualTo(3);
            Row farmingRow = plan.getRow(1);
            assertThat(farmingRow.getCell(3).getStringCellValue()).isEqualTo("Attività dell'agente");
            assertThat(farmingRow.getCell(4).getStringCellValue()).isEqualTo("Jinhsi");
            assertThat(farmingRow.getCell(5).getStringCellValue()).isEqualTo("Simulation Training: Resonance Potion");
            assertThat(farmingRow.getCell(7).getNumericCellValue()).isEqualTo(120);
            assertThat(plan.getRow(2).getCell(9).getStringCellValue()).isEqualTo("Fatta");
            assertThat(workbook.getSheet("Eventi").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Torneo di prova");
        }
    }

    @Test
    void exportAnswersTheErrorsInJsonLikeTheCalendar() throws Exception {
        mockMvc.perform(as(rover, export("2030-01-01", "2030-03-04")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.fields.to").value("L'intervallo può coprire al massimo 62 giorni."));
        mockMvc.perform(as(rover, get(EXPORT_URL).param("to", TUESDAY).header(HttpHeaders.ACCEPT, EXPORT_ACCEPT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void exportDoesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(export(MONDAY, TUESDAY).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private static MockHttpServletRequestBuilder export(String from, String to) {
        return get(EXPORT_URL).param("from", from).param("to", to).header(HttpHeaders.ACCEPT, EXPORT_ACCEPT);
    }

    private long manualTask(User user, String day, String taskStatus) {
        return jdbc.queryForObject("""
                insert into planned_task (user_id, origin, kind, status, task_date, title, minutes)
                values (?, 'MANUAL', 'CUSTOM', ?, ?::date, 'Attività manuale', 20) returning id""",
                Long.class, user.getId(), taskStatus, day);
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

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }
}
