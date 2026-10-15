package com.resonanceplanner.event;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.common.config.Today;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// The public events API (D49, D113) with the three demo events of events.json; "today" is 15 October 2026
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerTest {

    private static final String URL = "/api/events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameEventRepository gameEventRepository;

    @MockitoBean
    private Today today;

    @BeforeEach
    void setToday() {
        when(today.date()).thenReturn(LocalDate.of(2026, 10, 15));
    }

    @Test
    void listsEveryEditionOfTheMonthInOrderWithoutLogin() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Bountiful Crescendo twice: 4-10 and 18-24 October
                .andExpect(jsonPath("$.data[*].name").value(contains("By Moon's Grace",
                        "Lament Recon: Solaris Soldier", "Bountiful Crescendo", "Bountiful Crescendo")))
                .andExpect(jsonPath("$.data[*].startDate").value(contains("2026-09-21", "2026-09-29", "2026-10-04",
                        "2026-10-18")))
                .andExpect(jsonPath("$.data[*].endDate").value(contains("2026-11-03", "2026-11-06", "2026-10-10",
                        "2026-10-24")))
                // The whole edition of By Moon's Grace, but only the days of the range
                .andExpect(jsonPath("$.data[0].activeDates", hasSize(31)))
                .andExpect(jsonPath("$.data[0].activeDates[0]").value("2026-10-01"))
                .andExpect(jsonPath("$.data[2].activeDates", hasSize(7)))
                .andExpect(jsonPath("$.data[*].activeWeekdays").value(everyItem(is(nullValue()))))
                .andExpect(jsonPath("$.data[2].effect.type").value("DROP_MULTIPLIER"))
                .andExpect(jsonPath("$.data[2].effect.sourceType").value("SIMULATION_TRAINING"))
                .andExpect(jsonPath("$.data[2].effect.multiplier").value(2))
                .andExpect(jsonPath("$.data[2].effect.dailyLimit").value(3))
                .andExpect(jsonPath("$.data[1].effect").value(nullValue()))
                .andExpect(jsonPath("$.data[1].activities", hasSize(7)))
                .andExpect(jsonPath("$.data[1].activities[0].title").value("Drill 1"))
                .andExpect(jsonPath("$.data[1].activities[0].minutes").value(15))
                .andExpect(jsonPath("$.data[1].activities[0].frequency").value("ONCE"))
                .andExpect(jsonPath("$.data[0].rewards[0]").value("Palace Lantern da spendere nel negozio dell'evento"))
                .andExpect(jsonPath("$.data[0].description").isNotEmpty());
    }

    @Test
    void leavesOutAnEventBetweenTwoEditions() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-10-11").param("to", "2026-10-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(contains("By Moon's Grace", "Lament Recon: Solaris Soldier")));
    }

    @Test
    void acceptsARangeOfSixtyTwoDaysAndNotMore() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-10-01").param("to", "2026-12-01"))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL).param("from", "2026-10-01").param("to", "2026-12-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.to").value("L'intervallo può coprire al massimo 62 giorni."));
    }

    @Test
    void refusesARangeThatEndsBeforeItStarts() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-10-10").param("to", "2026-10-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.to").value("La data di fine non può venire prima di quella di inizio."));
    }

    @Test
    void refusesDatesOutsideTheWindowOfTheApp() throws Exception {
        mockMvc.perform(get(URL).param("from", "1999-12-31").param("to", "2000-01-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.from").value("Le date vanno dal 01/01/2000 al 31/12/2100."));
        mockMvc.perform(get(URL).param("from", "2100-12-20").param("to", "2101-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.to").exists());
        // A valid date near the limit of LocalDate: 400, not an overflow and a 500
        mockMvc.perform(get(URL).param("from", "+999999999-12-01").param("to", "+999999999-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.from").exists());
    }

    @Test
    void refusesAMissingOrWrongDateAsAnInvalidRequest() throws Exception {
        mockMvc.perform(get(URL).param("to", "2026-10-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(URL).param("from", "15/10/2026").param("to", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void showsTheNextEditionOfAnEventThatIsNotInProgress() throws Exception {
        Long bountiful = idOf("bountiful-crescendo");

        mockMvc.perform(get(URL + "/" + bountiful))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bountiful))
                .andExpect(jsonPath("$.data.startDate").value("2026-10-18"))
                .andExpect(jsonPath("$.data.endDate").value("2026-10-24"))
                .andExpect(jsonPath("$.data.activeDates", hasSize(7)));
    }

    @Test
    void showsTheEditionInProgressWithAllItsDays() throws Exception {
        mockMvc.perform(get(URL + "/" + idOf("lament-recon-solaris-soldier")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value("2026-09-29"))
                .andExpect(jsonPath("$.data.endDate").value("2026-11-06"))
                .andExpect(jsonPath("$.data.activeDates", hasSize(39)));
    }

    @Test
    void answersNotFoundForAnUnknownEvent() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Evento non trovato."));
        mockMvc.perform(get(URL + "/abc")).andExpect(status().isBadRequest());
    }

    private Long idOf(String code) {
        return gameEventRepository.findByCode(code).orElseThrow().getId();
    }
}
