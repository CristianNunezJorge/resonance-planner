package com.resonanceplanner.event.dto;

import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.ScheduleType;
import com.resonanceplanner.event.dto.EventResponse.Activity;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * An event as the admin sees it: the schedule as it is stored, not the computed editions (D14). {@code code} is the
 * identifier of events.json for the demo events and null for those created by the admin (D39).
 */
public record AdminEventResponse(Long id, String code, String name, EventType type, String description,
        Schedule schedule, List<DayOfWeek> activeWeekdays, List<String> rewards, List<Activity> activities,
        Effect effect) {

    public static AdminEventResponse from(GameEvent event) {
        Schedule schedule = new Schedule(event.getScheduleType(), event.getStartDate(), event.getEndDate(),
                event.getStartOffsetDays(), event.getDurationDays(), event.getRepeatEveryDays());
        List<DayOfWeek> weekdays = event.getActiveWeekdays().isEmpty()
                ? null
                : event.getActiveWeekdays().stream().sorted().toList();
        return new AdminEventResponse(event.getId(), event.getCode(), event.getName(), event.getType(),
                event.getDescription(), schedule, weekdays, List.copyOf(event.getRewards()),
                event.getActivities().stream().map(Activity::from).toList(), Effect.from(event.getEffect()));
    }

    /** FIXED has the two dates, RECURRING the three numbers (D29); the others are null. */
    public record Schedule(ScheduleType type, LocalDate startDate, LocalDate endDate, Integer startOffsetDays,
            Integer durationDays, Integer repeatEveryDays) {}
}
