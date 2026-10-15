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
}
