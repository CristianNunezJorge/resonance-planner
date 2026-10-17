package com.resonanceplanner.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.testsupport.MutableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

// "Today" changes at midnight in Italy, not at midnight UTC (D41)
class TodayTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    @Test
    void startsTheDayAtMidnightInItalyInSummerTime() {
        // 22:30 UTC on 14 October is 00:30 of 15 October in Rome (UTC+2)
        Today today = new Today(new MutableClock(Instant.parse("2026-10-14T22:30:00Z")), ROME);

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void keepsTheSameDayJustBeforeMidnightInWinterTime() {
        // 22:59 UTC on 14 December is 23:59 of the same day in Rome (UTC+1)
        Today today = new Today(new MutableClock(Instant.parse("2026-12-14T22:59:00Z")), ROME);

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 12, 14));
    }

    @Test
    void startsAndEndsTheDayAtMidnightInItalyForTheDailyLimits() {
        // 15 October in Rome (UTC+2) goes from 22:00 UTC of the 14th to 22:00 UTC of the 15th (D129)
        Today today = new Today(new MutableClock(Instant.parse("2026-10-15T09:00:00Z")), ROME);

        assertThat(today.startOfDay()).isEqualTo(Instant.parse("2026-10-14T22:00:00Z"));
        assertThat(today.startOfNextDay()).isEqualTo(Instant.parse("2026-10-15T22:00:00Z"));
    }

    @Test
    void givesTheNextMidnightAfterTheChangeOfTime() {
        // 25 October 2026 has 25 hours in Rome: it starts in summer time and ends in winter time
        Today today = new Today(new MutableClock(Instant.parse("2026-10-25T12:00:00Z")), ROME);

        assertThat(today.startOfDay()).isEqualTo(Instant.parse("2026-10-24T22:00:00Z"));
        assertThat(today.startOfNextDay()).isEqualTo(Instant.parse("2026-10-25T23:00:00Z"));
    }

    @Test
    void givesTheItalianDayOfAnyMoment() {
        Today today = new Today(new MutableClock(Instant.parse("2026-12-14T10:00:00Z")), ROME);

        assertThat(today.dateOf(Instant.parse("2026-10-14T22:30:00Z"))).isEqualTo(LocalDate.of(2026, 10, 15));
    }
}
