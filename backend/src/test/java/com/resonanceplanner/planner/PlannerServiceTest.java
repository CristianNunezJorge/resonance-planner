package com.resonanceplanner.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.planner.ScheduleDraft.DayPlan;
import com.resonanceplanner.planner.ScheduleDraft.DraftTask;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// What the planner reads for the user of the token (D44, D123), on the Docker Compose test database; every test is
// rolled back. Today is a fixed Monday and the demo events are removed: every event here is written by the test.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlannerServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 14);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final ScheduleRequest DEFAULTS = ScheduleRequest.defaults();

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private Today today;

    private User rover;

    @BeforeEach
    void createAUserAndRemoveTheDemoEvents() {
        when(today.date()).thenReturn(MONDAY);
        User user = User.create("rover", "rover@example.com", "hash", Role.USER);
        user.changePlayTimeMinutes(60);
        user.recordWaveplate(new WaveplateSnapshot(100, 20, Instant.now()));
        rover = userRepository.saveAndFlush(user);
        jdbc.update("delete from game_event");
    }

    @Test
    void plansAWeekFromTodayWithThePlayTimeOfTheUser() {
        addToRoster(rover, "jinhsi");
        addToRoster(rover, "yinlin");

        ScheduleDraft draft = plannerService.draft(rover.getId(), DEFAULTS);

        assertThat(draft.days()).hasSize(PlannerService.DEFAULT_DAYS);
        assertThat(draft.days().getFirst().date()).isEqualTo(MONDAY);
        assertThat(draft.characterPriority()).containsExactly("jinhsi", "yinlin");
        // Today the Waveplate of the reading plus the Crystals, then 240 a day
        assertThat(draft.days()).extracting(DayPlan::waveplatesAvailable).startsWith(120, 240, 240);
        assertThat(draft.days()).extracting(DayPlan::minutesAvailable).allMatch(minutes -> minutes == 60);
        assertThat(draft.days().getFirst().tasks().getFirst())
                .extracting(DraftTask::farmingSourceId, DraftTask::characterId)
                .containsExactly("boss-mourning-aix", "jinhsi");
    }

    @Test
    void followsThePriorityAndPutsTheOtherCharactersAfter() {
        addToRoster(rover, "jinhsi");
        addToRoster(rover, "yinlin");

        ScheduleDraft draft = plannerService.draft(rover.getId(),
                new ScheduleRequest(TUESDAY, 3, List.of("yinlin"), List.of(), null, 90));

        assertThat(draft.characterPriority()).containsExactly("yinlin", "jinhsi");
        assertThat(draft.days()).extracting(DayPlan::date)
                .containsExactly(TUESDAY, TUESDAY.plusDays(1), TUESDAY.plusDays(2));
        // Not today: a whole day of Waveplate, no Crystals
        assertThat(draft.days()).extracting(DayPlan::waveplatesAvailable).containsOnly(240);
        assertThat(draft.days()).extracting(DayPlan::minutesAvailable).containsOnly(90);
        assertThat(draft.days().getFirst().tasks().getFirst().characterId()).isEqualTo("yinlin");
    }

    @Test
    void usesAnHourWhenThePlayTimeIsNotChosenYet() {
        User newcomer = userRepository.saveAndFlush(User.create("newcomer", "newcomer@example.com", "hash", Role.USER));

        ScheduleDraft draft = plannerService.draft(newcomer.getId(), DEFAULTS);

        assertThat(draft.days()).extracting(DayPlan::minutesAvailable).containsOnly(PlannerService.DEFAULT_MINUTES);
        assertThat(draft.days().getFirst().waveplatesAvailable()).isEqualTo(240);
    }

    @Test
    void leavesOutTheExcludedDays() {
        ScheduleDraft draft = plannerService.draft(rover.getId(),
                new ScheduleRequest(null, 3, null, List.of(TUESDAY, MONDAY.minusDays(5)), null, null));

        assertThat(draft.days()).extracting(DayPlan::excluded).containsExactly(false, true, false);
    }

    @Nested
    class TasksAlreadyInTheCalendar {

        @Test
        void takeTheirMinutesWhenTheyAreManualOrDoneButNotWhenTheyWillBeReplaced() {
            manualTask(rover, MONDAY, 20);
            agentTask(rover, "CUSTOM", "DONE", MONDAY, null, null, "Fatta", null);
            agentTask(rover, "CUSTOM", "CONFIRMED", TUESDAY, null, null, "Da sostituire", null);

            ScheduleDraft draft = plannerService.draft(rover.getId(), new ScheduleRequest(null, 2, null, null, null, null));

            assertThat(draft.days()).extracting(DayPlan::minutesAvailable).containsExactly(30, 60);
        }

        @Test
        void useUpTheDailyLimitOfAnEventWhenTheyAreDone() {
            addToRoster(rover, "jinhsi");
            doubleDrops(MONDAY, FarmingSourceType.SIMULATION_TRAINING, 3);
            agentTask(rover, "FARMING", "DONE", MONDAY, "sim-resonance-potion", 2, "Simulation Training", null);

            ScheduleDraft draft = plannerService.draft(rover.getId(), new ScheduleRequest(null, 1, null, null, null, null));

            assertThat(draft.days().getFirst().tasks())
                    .filteredOn(task -> "sim-resonance-potion".equals(task.farmingSourceId()))
                    .extracting(DraftTask::boostedRuns)
                    .containsExactly(1);
        }

        @Test
        void areNotRead() {
            User other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
            manualTask(other, MONDAY, 50);

            ScheduleDraft draft = plannerService.draft(rover.getId(), new ScheduleRequest(null, 1, null, null, null, null));

            assertThat(draft.days().getFirst().minutesAvailable()).isEqualTo(60);
        }
    }

    @Nested
    class EventActivities {

        private GameEvent festival;

        @BeforeEach
        void createAFestivalFromSundayToWednesday() {
            festival = GameEvent.custom("Festa", EventType.POINTS_SHOP, "Solo per i test.");
            festival.scheduleFixed(MONDAY.minusDays(1), MONDAY.plusDays(2));
            festival.addActivity("Obiettivi", 10, ActivityFrequency.DAILY, null);
            festival.addActivity("Drill 1", 10, ActivityFrequency.ONCE, null);
            // From the third day of the edition: Tuesday
            festival.addActivity("Drill 2", 10, ActivityFrequency.ONCE, 3);
            festival = gameEventRepository.saveAndFlush(festival);
        }

        @Test
        void arePlannedOnTheirDaysFromTheUnlockDay() {
            ScheduleDraft draft = plannerService.draft(rover.getId(), new ScheduleRequest(null, 4, null, null, null, null));

            assertThat(eventTitles(draft.days().get(0))).containsExactly("Festa: Obiettivi", "Festa: Drill 1");
            assertThat(eventTitles(draft.days().get(1))).containsExactly("Festa: Obiettivi", "Festa: Drill 2");
            assertThat(eventTitles(draft.days().get(2))).containsExactly("Festa: Obiettivi");
            // Thursday: the edition is over
            assertThat(eventTitles(draft.days().get(3))).isEmpty();
            assertThat(draft.days().getFirst().tasks().getFirst().eventId()).isEqualTo(festival.getId());
        }

        @Test
        void areLeftOutWhenTheyAreAlreadyDone() {
            // Drill 1 done on Sunday, in the same edition; the daily one done today
            agentTask(rover, "EVENT", "DONE", MONDAY.minusDays(1), null, null, "Festa: Drill 1", festival.getId());
            agentTask(rover, "EVENT", "DONE", MONDAY, null, null, "Festa: Obiettivi", festival.getId());

            ScheduleDraft draft = plannerService.draft(rover.getId(), new ScheduleRequest(null, 2, null, null, null, null));

            assertThat(eventTitles(draft.days().get(0))).isEmpty();
            assertThat(eventTitles(draft.days().get(1))).containsExactly("Festa: Obiettivi", "Festa: Drill 2");
        }

        @Test
        void areLeftOutOnRequest() {
            ScheduleDraft draft = plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, 2, null, null, false, null));

            assertThat(draft.days()).allMatch(day -> eventTitles(day).isEmpty());
        }

        private List<String> eventTitles(DayPlan day) {
            return day.tasks().stream().filter(task -> task.kind() == TaskKind.EVENT).map(DraftTask::title).toList();
        }
    }

    @Nested
    class Validation {

        @Test
        void refusesEveryWrongParameterInOneAnswer() {
            addToRoster(rover, "jinhsi");

            assertThatThrownBy(() -> plannerService.draft(rover.getId(), new ScheduleRequest(MONDAY.minusDays(1), 15,
                    List.of("changli"), null, null, 10)))
                    .isInstanceOfSatisfying(FieldValidationException.class, error -> assertThat(error.getFields())
                            .containsOnlyKeys("startDate", "days", "characterPriority", "maxMinutesPerDay"));
        }

        @Test
        void refusesTheLimitsJustOutside() {
            assertThatThrownBy(() -> plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, 0, null, null, null, 241)))
                    .isInstanceOfSatisfying(FieldValidationException.class, error -> assertThat(error.getFields())
                            .containsOnlyKeys("days", "maxMinutesPerDay"));
        }

        @Test
        void acceptsTheLimits() {
            ScheduleDraft longest = plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, 14, null, null, null, 240));
            ScheduleDraft shortest = plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, 1, null, null, null, 15));

            assertThat(longest.days()).hasSize(14);
            assertThat(shortest.days()).extracting(DayPlan::minutesAvailable).containsExactly(15);
        }

        @Test
        void refusesMoreExcludedDaysThanAPlanCanHave() {
            List<LocalDate> fifteenDays = MONDAY.datesUntil(MONDAY.plusDays(15)).toList();

            assertThatThrownBy(() -> plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, null, null, fifteenDays, null, null)))
                    .isInstanceOfSatisfying(FieldValidationException.class, error -> assertThat(error.getFields())
                            .containsEntry("excludedDates", "Puoi escludere al massimo 14 giorni."));
        }

        @Test
        void refusesACharacterNamedTwice() {
            addToRoster(rover, "jinhsi");

            assertThatThrownBy(() -> plannerService.draft(rover.getId(),
                    new ScheduleRequest(null, null, List.of("jinhsi", "jinhsi"), null, null, null)))
                    .isInstanceOfSatisfying(FieldValidationException.class, error -> assertThat(error.getFields())
                            .containsEntry("characterPriority", "Ogni personaggio può comparire una volta sola."));
        }

        @Test
        void refusesAStartFarFromToday() {
            assertThatThrownBy(() -> plannerService.draft(rover.getId(),
                    new ScheduleRequest(LocalDate.of(2200, 1, 1), null, null, null, null, null)))
                    .isInstanceOfSatisfying(FieldValidationException.class, error -> assertThat(error.getFields())
                            .containsOnlyKeys("startDate"));
        }

        @Test
        void answers401WhenTheUserOfTheTokenDoesNotExistAnymore() {
            assertThatThrownBy(() -> plannerService.draft(-1L, DEFAULTS)).isInstanceOf(UnauthorizedException.class);
        }
    }

    @Test
    void leavesTheExcludedDaysOfTheRequestThatAreOutsideThePlan() {
        ScheduleDraft draft = plannerService.draft(rover.getId(),
                new ScheduleRequest(null, 1, null, List.of(MONDAY.plusDays(30)), null, null));

        assertThat(draft.days()).extracting(DayPlan::date, DayPlan::excluded).containsExactly(tuple(MONDAY, false));
    }

    private void addToRoster(User user, String characterId) {
        jdbc.update("""
                insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                    target_rank)
                values (?, ?, 1, 0, 40, 2)""", user.getId(), characterId);
    }

    private void manualTask(User user, LocalDate day, int minutes) {
        jdbc.update("""
                insert into planned_task (user_id, origin, kind, status, task_date, title, minutes)
                values (?, 'MANUAL', 'CUSTOM', 'CONFIRMED', ?, 'Attività manuale', ?)""", user.getId(), day, minutes);
    }

    // Tasks of the agent exist only inside a proposal (D35): until the proposals arrive (T30) they are written with SQL
    private void agentTask(User user, String kind, String taskStatus, LocalDate day, String sourceId, Integer runs,
            String title, Long eventId) {
        Long proposalId = jdbc.queryForObject(
                "insert into plan_proposal (user_id, status) values (?, 'CONFIRMED') returning id", Long.class,
                user.getId());
        jdbc.update("""
                insert into planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes,
                    farming_source_id, runs, event_id)
                values (?, ?, 'AGENT', ?, ?, ?, ?, 10, ?, ?, ?)""",
                user.getId(), proposalId, kind, taskStatus, day, title, sourceId, runs, eventId);
    }

    private void doubleDrops(LocalDate day, FarmingSourceType type, Integer dailyLimit) {
        GameEvent event = GameEvent.custom("Ricompense doppie di prova", EventType.DOUBLE_DROP, "Solo per i test.");
        event.scheduleFixed(day, day);
        event.setActiveWeekdays(Set.of());
        event.setEffect(new EventEffect(EffectType.DROP_MULTIPLIER, type, new BigDecimal("2"), dailyLimit));
        gameEventRepository.saveAndFlush(event);
    }
}
