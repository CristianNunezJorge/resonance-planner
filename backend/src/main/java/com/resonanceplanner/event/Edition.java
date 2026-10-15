package com.resonanceplanner.event;

import java.time.LocalDate;
import java.util.List;

/**
 * One run of an event, from its first to its last day, both included (D29), with the days on which it is really
 * active (D96): inside the requested range for a list, the whole edition for the detail (D113).
 */
public record Edition(LocalDate start, LocalDate end, List<LocalDate> activeDates) {

    public Edition {
        activeDates = List.copyOf(activeDates);
    }
}
