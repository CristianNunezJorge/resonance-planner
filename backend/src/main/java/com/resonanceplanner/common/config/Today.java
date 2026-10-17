package com.resonanceplanner.common.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Today's date in the time zone of the app (D41): the moments are in UTC, but a day starts and ends at midnight in
 * Italy. It reads the instant of the clock and converts it, so a test clock in UTC works too.
 */
@Component
public class Today {

    private final Clock clock;
    private final ZoneId zone;

    public Today(Clock clock, ZoneId appZone) {
        this.clock = clock;
        this.zone = appZone;
    }

    public LocalDate date() {
        return dateOf(clock.instant());
    }

    /** Midnight of today in the time zone of the app: the daily limits count from here (D129). */
    public Instant startOfDay() {
        return date().atStartOfDay(zone).toInstant();
    }

    /** The next midnight in the time zone of the app, when the daily limits start again (D129). */
    public Instant startOfNextDay() {
        return date().plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** The day of a moment in the time zone of the app, for example the day a proposal was made (D124). */
    public LocalDate dateOf(Instant instant) {
        return LocalDate.ofInstant(instant, zone);
    }
}
