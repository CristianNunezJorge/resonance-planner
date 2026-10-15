package com.resonanceplanner.event;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.testsupport.TestGameRules;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Editions and active days (D29, D96, D113). The anchor date is Thursday 1 October 2026
class EventCalendarTest {

    // Bountiful Crescendo: from Sunday 4 to Saturday 10 October, then every 14 days
    private static final EventSchedule BOUNTIFUL = EventSchedule.recurring(3, 7, 14, Set.of());
    private static final EventSchedule WEEKENDS = EventSchedule.recurring(3, 7, 14, Set.of(SATURDAY, SUNDAY));
    // Lament Recon: from 29 September to 6 November, then every 42 days
    private static final EventSchedule LAMENT = EventSchedule.recurring(-2, 39, 42, Set.of());
    private static final EventSchedule FIXED = EventSchedule.fixed(day("10-12"), day("10-14"), Set.of());
    // One edition right after the other: 1-7 October, 8-14 October...
    private static final EventSchedule CONTINUOUS = EventSchedule.recurring(0, 7, 7, Set.of());

    private final EventCalendar calendar = new EventCalendar(TestGameRules.standard());

    private static LocalDate day(String monthAndDay) {
        return LocalDate.parse("2026-" + monthAndDay);
    }

    private static List<LocalDate> days(String first, String last) {
        return Stream.iterate(day(first), date -> !date.isAfter(day(last)), date -> date.plusDays(1)).toList();
    }

    private static Edition edition(String start, String end, List<LocalDate> activeDates) {
        return new Edition(day(start), day(end), activeDates);
    }

    private List<Edition> between(EventSchedule schedule, String from, String to) {
        return calendar.between(schedule, day(from), day(to));
    }

    @Test
    void hasNoEditionBeforeItStarts() {
        assertThat(between(BOUNTIFUL, "10-01", "10-03")).isEmpty();
    }

    @Test
    void findsTheEditionInProgressWithItsWholeDatesAndTheActiveDaysOfTheRange() {
        assertThat(between(BOUNTIFUL, "10-05", "10-06"))
                .containsExactly(edition("10-04", "10-10", days("10-05", "10-06")));
    }

    @Test
    void hasNoEditionAfterItEndsAndBeforeTheNextOne() {
        assertThat(between(BOUNTIFUL, "10-11", "10-17")).isEmpty();
    }

    @Test
    void includesTheFirstAndTheLastDayOfAnEdition() {
        assertThat(between(BOUNTIFUL, "10-10", "10-18")).containsExactly(
                edition("10-04", "10-10", List.of(day("10-10"))),
                edition("10-18", "10-24", List.of(day("10-18"))));
    }

    @Test
    void returnsEveryEditionOfALongRangeInOrder() {
        assertThat(between(BOUNTIFUL, "10-01", "10-31")).containsExactly(
                edition("10-04", "10-10", days("10-04", "10-10")),
                edition("10-18", "10-24", days("10-18", "10-24")));
    }

    @Test
    void findsTheEditionsBeforeTheAnchorDate() {
        assertThat(between(BOUNTIFUL, "09-15", "09-30"))
                .containsExactly(edition("09-20", "09-26", days("09-20", "09-26")));
    }

    @Test
    void keepsOnlyTheActiveDaysOfTheWeek() {
        assertThat(between(WEEKENDS, "10-01", "10-31")).containsExactly(
                edition("10-04", "10-10", List.of(day("10-04"), day("10-10"))),
                edition("10-18", "10-24", List.of(day("10-18"), day("10-24"))));
    }

    @Test
    void leavesOutAnEditionWithoutActiveDaysInTheRange() {
        // The edition runs from Sunday to Saturday, but from Monday to Wednesday the weekend event is not active
        assertThat(between(WEEKENDS, "10-05", "10-07")).isEmpty();
    }

    @Test
    void findsALongEventInARangeOfOneDay() {
        assertThat(between(LAMENT, "10-15", "10-15")).containsExactly(edition("09-29", "11-06", List.of(day("10-15"))));
    }

    @Test
    void returnsTwoEditionsThatTouchEachOther() {
        assertThat(between(CONTINUOUS, "10-06", "10-09")).containsExactly(
                edition("10-01", "10-07", days("10-06", "10-07")),
                edition("10-08", "10-14", days("10-08", "10-09")));
    }

    @Test
    void findsAFixedEventOnlyInsideItsDates() {
        assertThat(between(FIXED, "10-01", "10-11")).isEmpty();
        assertThat(between(FIXED, "10-13", "10-20")).containsExactly(edition("10-12", "10-14", days("10-13", "10-14")));
        assertThat(between(FIXED, "10-15", "10-30")).isEmpty();
    }

    @Test
    void picksTheEditionInProgressFromItsFirstToItsLastDay() {
        Edition first = edition("10-04", "10-10", days("10-04", "10-10"));

        assertThat(calendar.currentOrNext(BOUNTIFUL, day("10-04"))).isEqualTo(first);
        assertThat(calendar.currentOrNext(BOUNTIFUL, day("10-07"))).isEqualTo(first);
        assertThat(calendar.currentOrNext(BOUNTIFUL, day("10-10"))).isEqualTo(first);
        assertThat(calendar.currentOrNext(LAMENT, day("10-15"))).isEqualTo(edition("09-29", "11-06", days("09-29", "11-06")));
    }

    @Test
    void picksTheNextEditionBetweenTwoEditions() {
        assertThat(calendar.currentOrNext(BOUNTIFUL, day("10-11"))).isEqualTo(edition("10-18", "10-24", days("10-18", "10-24")));
        assertThat(calendar.currentOrNext(BOUNTIFUL, day("10-02"))).isEqualTo(edition("10-04", "10-10", days("10-04", "10-10")));
        assertThat(calendar.currentOrNext(WEEKENDS, day("10-15")))
                .isEqualTo(edition("10-18", "10-24", List.of(day("10-18"), day("10-24"))));
    }

    @Test
    void picksAnEditionLongBeforeTheAnchorDate() {
        Edition edition = calendar.currentOrNext(BOUNTIFUL, LocalDate.of(2026, 1, 1));

        assertThat(edition.start()).isEqualTo(LocalDate.of(2025, 12, 28));
        assertThat(edition.end()).isEqualTo(LocalDate.of(2026, 1, 3));
    }

    @Test
    void findsTheEditionBeforeOneThatStartsBeforeTheAnchorDate() {
        assertThat(between(LAMENT, "08-15", "09-30")).containsExactly(
                edition("08-18", "09-25", days("08-18", "09-25")),
                edition("09-29", "11-06", days("09-29", "09-30")));
    }

    @Test
    void refusesAScheduleThatCouldLoopForeverOrDivideByZero() {
        assertThatThrownBy(() -> EventSchedule.recurring(0, 7, 6, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventSchedule.recurring(0, 0, 0, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventSchedule.fixed(day("10-14"), day("10-12"), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsTheDatesOfAFixedEventBeforeAndAfterIt() {
        Edition fixed = edition("10-12", "10-14", days("10-12", "10-14"));

        assertThat(calendar.currentOrNext(FIXED, day("10-01"))).isEqualTo(fixed);
        assertThat(calendar.currentOrNext(FIXED, day("10-30"))).isEqualTo(fixed);
    }
}
