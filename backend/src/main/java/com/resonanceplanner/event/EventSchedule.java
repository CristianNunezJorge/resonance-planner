package com.resonanceplanner.event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * When an event runs, as plain values (D29, D96): the dates of a FIXED event, or the offset from the anchor date,
 * the duration and the repetition of a RECURRING one, plus the days of the week (none = every day).
 */
public record EventSchedule(
        ScheduleType type,
        LocalDate startDate,
        LocalDate endDate,
        Integer startOffsetDays,
        Integer durationDays,
        Integer repeatEveryDays,
        Set<DayOfWeek> activeWeekdays) {

    // The database and EventRules already refuse these values: here they would loop forever or divide by zero
    public EventSchedule {
        boolean possible = type == ScheduleType.FIXED
                ? startDate != null && endDate != null && !endDate.isBefore(startDate)
                : startOffsetDays != null && durationDays != null && repeatEveryDays != null
                        && durationDays >= 1 && repeatEveryDays >= durationDays;
        if (!possible) {
            throw new IllegalArgumentException("Impossible schedule of an event: " + type + " " + startDate + " "
                    + endDate + " " + startOffsetDays + " " + durationDays + " " + repeatEveryDays);
        }
        activeWeekdays = activeWeekdays.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(activeWeekdays));
    }

    public static EventSchedule fixed(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> activeWeekdays) {
        return new EventSchedule(ScheduleType.FIXED, startDate, endDate, null, null, null, activeWeekdays);
    }

    public static EventSchedule recurring(int startOffsetDays, int durationDays, int repeatEveryDays,
            Set<DayOfWeek> activeWeekdays) {
        return new EventSchedule(ScheduleType.RECURRING, null, null, startOffsetDays, durationDays, repeatEveryDays,
                activeWeekdays);
    }

    public static EventSchedule of(GameEvent event) {
        return new EventSchedule(event.getScheduleType(), event.getStartDate(), event.getEndDate(),
                event.getStartOffsetDays(), event.getDurationDays(), event.getRepeatEveryDays(),
                event.getActiveWeekdays());
    }

    /** True when the event runs on that day of the week. */
    boolean isActiveOn(DayOfWeek day) {
        return activeWeekdays.isEmpty() || activeWeekdays.contains(day);
    }
}
