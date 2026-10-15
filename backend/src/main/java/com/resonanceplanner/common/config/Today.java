package com.resonanceplanner.common.config;

import java.time.Clock;
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
        return LocalDate.ofInstant(clock.instant(), zone);
    }
}
