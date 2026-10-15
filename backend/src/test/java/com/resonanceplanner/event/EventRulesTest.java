package com.resonanceplanner.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.event.dto.EventRequest.Activity;
import com.resonanceplanner.event.dto.EventRequest.Effect;
import com.resonanceplanner.event.dto.EventRequest.Schedule;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// The rules of D114 that join several fields; the shape of each value is checked by the annotations of EventRequest
class EventRulesTest {

    private static final Effect DOUBLE_SIMULATION = new Effect(EffectType.DROP_MULTIPLIER,
            FarmingSourceType.SIMULATION_TRAINING, new BigDecimal("2"), 3);

    private final EventRules rules = new EventRules();

    private static Schedule fixed(String start, String end) {
        return new Schedule(ScheduleType.FIXED, start == null ? null : LocalDate.parse(start),
                end == null ? null : LocalDate.parse(end), null, null, null);
    }

    private static Schedule recurring(Integer offset, Integer duration, Integer repeat) {
        return new Schedule(ScheduleType.RECURRING, null, null, offset, duration, repeat);
    }

    private static Activity unlockingOn(Integer day) {
        return new Activity("Drill", 15, ActivityFrequency.ONCE, day);
    }

    private static EventRequest event(EventType type, Schedule schedule, List<Activity> activities, Effect effect) {
        return new EventRequest("Evento di prova", type, "Descrizione", schedule, null, null, activities, effect);
    }

    private static EventRequest combat(Schedule schedule) {
        return event(EventType.COMBAT_STAGES, schedule, List.of(), null);
    }

    private static EventRequest combatOn(Schedule schedule, Set<DayOfWeek> weekdays) {
        return new EventRequest("Evento di prova", EventType.COMBAT_STAGES, "Descrizione", schedule, weekdays, null,
                List.of(), null);
    }

    @Test
    void refusesFixedDatesOutsideTheWindowOfTheApp() {
        assertThat(rules.problems(combat(fixed("1999-12-31", "2000-01-05"))))
                .containsOnlyKeys("schedule.startDate")
                .containsEntry("schedule.startDate", "Le date vanno dal 01/01/2000 al 31/12/2100.");
        assertThat(rules.problems(combat(fixed("2100-12-30", "2101-01-01")))).containsOnlyKeys("schedule.endDate");
        Schedule farAway = new Schedule(ScheduleType.FIXED, LocalDate.MAX.minusDays(3), LocalDate.MAX, null, null, null);
        assertThat(rules.problems(combat(farAway))).containsOnlyKeys("schedule.startDate");
        assertThat(rules.problems(combat(fixed("2000-01-01", "2000-01-02")))).isEmpty();
        assertThat(rules.problems(combat(fixed("2100-12-30", "2100-12-31")))).isEmpty();
    }

    @Test
    void refusesDaysOfTheWeekThatAFixedEventNeverHas() {
        // Monday 12 to Wednesday 14 October: no Saturday inside
        Schedule mondayToWednesday = fixed("2026-10-12", "2026-10-14");

        assertThat(rules.problems(combatOn(mondayToWednesday, Set.of(DayOfWeek.SATURDAY))))
                .containsEntry("activeWeekdays", "Nessuno dei giorni scelti cade tra l'inizio e la fine dell'evento.");
        assertThat(rules.problems(combatOn(mondayToWednesday, Set.of(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY)))).isEmpty();
        assertThat(rules.problems(combatOn(mondayToWednesday, EnumSet.allOf(DayOfWeek.class)))).isEmpty();
    }

    @Test
    void refusesDaysOfTheWeekForARecurringEventShorterThanAWeek() {
        // Editions of 3 days every 10 days move through the week: a chosen day could be missing from some of them
        assertThat(rules.problems(combatOn(recurring(0, 3, 10), Set.of(DayOfWeek.SATURDAY))))
                .containsOnlyKeys("activeWeekdays");
        assertThat(rules.problems(combatOn(recurring(0, 3, 10), EnumSet.allOf(DayOfWeek.class)))).isEmpty();
        assertThat(rules.problems(combatOn(recurring(0, 7, 14), Set.of(DayOfWeek.SUNDAY)))).isEmpty();
    }

    @Test
    void acceptsAFixedEventAndARecurringOneWithItsEffect() {
        assertThat(rules.problems(combat(fixed("2026-10-12", "2026-10-14")))).isEmpty();
        assertThat(rules.problems(event(EventType.DOUBLE_DROP, recurring(3, 7, 14), List.of(), DOUBLE_SIMULATION)))
                .isEmpty();
    }

    @Test
    void ignoresTheValuesOfTheOtherKindOfSchedule() {
        Schedule both = new Schedule(ScheduleType.FIXED, LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-14"),
                99_999, -5, 0);

        assertThat(rules.problems(combat(both))).isEmpty();
    }

    @Test
    void needsBothDatesOfAFixedEvent() {
        assertThat(rules.problems(combat(fixed(null, null))))
                .containsOnlyKeys("schedule.startDate", "schedule.endDate");
        assertThat(rules.problems(combat(fixed("2026-10-12", null)))).containsOnlyKeys("schedule.endDate");
    }

    @Test
    void refusesAFixedEventThatEndsBeforeItStarts() {
        assertThat(rules.problems(combat(fixed("2026-10-14", "2026-10-13"))))
                .containsEntry("schedule.endDate", "La fine non può venire prima dell'inizio.");
        assertThat(rules.problems(combat(fixed("2026-10-14", "2026-10-14")))).isEmpty();
    }

    @Test
    void acceptsAFixedEventOfAYearAndNotMore() {
        // 1 January to 1 January of the next year is 366 days, last day included
        assertThat(rules.problems(combat(fixed("2027-01-01", "2028-01-01")))).isEmpty();
        assertThat(rules.problems(combat(fixed("2027-01-01", "2028-01-02"))))
                .containsEntry("schedule.endDate", "Un evento può durare al massimo 366 giorni.");
    }

    @Test
    void needsTheThreeNumbersOfARecurringEvent() {
        assertThat(rules.problems(combat(recurring(null, null, null))))
                .containsOnlyKeys("schedule.startOffsetDays", "schedule.durationDays", "schedule.repeatEveryDays");
    }

    @ParameterizedTest
    @CsvSource({
        "3651,7,14,schedule.startOffsetDays",
        "-3651,7,14,schedule.startOffsetDays",
        "0,0,14,schedule.durationDays",
        "0,367,400,schedule.durationDays",
        "0,7,6,schedule.repeatEveryDays",
        "0,7,3651,schedule.repeatEveryDays",
    })
    void refusesTheNumbersOfARecurringEventOutsideTheirRange(int offset, int duration, int repeat, String field) {
        assertThat(rules.problems(combat(recurring(offset, duration, repeat)))).containsOnlyKeys(field);
    }

    @ParameterizedTest
    @CsvSource({"-3650,1,1", "3650,366,3650", "0,7,7"})
    void acceptsTheLimitsOfARecurringEvent(int offset, int duration, int repeat) {
        assertThat(rules.problems(combat(recurring(offset, duration, repeat)))).isEmpty();
    }

    @Test
    void refusesAnActivityThatUnlocksAfterTheLastDay() {
        // 12-14 October: three days
        EventRequest request = event(EventType.COMBAT_STAGES, fixed("2026-10-12", "2026-10-14"),
                List.of(unlockingOn(null), unlockingOn(3), unlockingOn(4)), null);

        assertThat(rules.problems(request))
                .containsOnlyKeys("activities[2].unlockDay")
                .containsEntry("activities[2].unlockDay", "Il giorno di sblocco va da 1 a 3, la durata dell'evento.");
        assertThat(rules.problems(event(EventType.COMBAT_STAGES, recurring(0, 7, 14), List.of(unlockingOn(8)), null)))
                .containsOnlyKeys("activities[0].unlockDay");
    }

    @Test
    void doesNotJudgeTheUnlockDayWhenTheDurationIsWrong() {
        EventRequest request = event(EventType.COMBAT_STAGES, recurring(0, 0, 14), List.of(unlockingOn(30)), null);

        assertThat(rules.problems(request)).containsOnlyKeys("schedule.durationDays");
    }

    @Test
    void needsTheEffectForDoubleDropsAndOnlyForThem() {
        assertThat(rules.problems(event(EventType.DOUBLE_DROP, recurring(3, 7, 14), List.of(), null)))
                .containsEntry("effect", "Un evento di ricompense doppie ha bisogno dell'effetto.");
        assertThat(rules.problems(event(EventType.POINTS_SHOP, recurring(3, 7, 14), List.of(), DOUBLE_SIMULATION)))
                .containsEntry("effect", "Solo gli eventi di ricompense doppie hanno un effetto.");
    }

    @Test
    void namesEveryWrongFieldInTheOrderOfTheForm() {
        EventRequest request = event(EventType.POINTS_SHOP, fixed("2026-10-14", "2026-10-12"),
                List.of(unlockingOn(2)), DOUBLE_SIMULATION);

        // The unlock day is not judged, because the dates are wrong
        assertThat(rules.problems(request)).containsOnlyKeys("schedule.endDate", "effect");
        assertThat(rules.problems(request).keySet()).containsExactly("schedule.endDate", "effect");
    }
}
