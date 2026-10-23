package com.resonanceplanner.event.dto;

import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.ScheduleType;
import com.resonanceplanner.event.dto.EventResponse.Activity;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * An event as the admin sees it: the schedule as it is stored, not the computed editions (D14). {@code code} is the
 * identifier of events.json for the demo events and null for those created by the admin (D39).
 */
public record AdminEventResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "Identificativo in events.json per gli eventi della demo; null per quelli creati "
                + "dall'admin", nullable = true, example = "bountiful-crescendo")
        String code,
        @Schema(example = "Bountiful Crescendo") String name,
        @Schema(example = "DOUBLE_DROP") EventType type,
        @Schema(example = "Simulation Training e Forgery Challenge danno ricompense doppie, fino a 3 volte al giorno.")
        String description,
        Schedule schedule,
        @Schema(description = "Giorni della settimana in cui è attivo; null = tutti", nullable = true, example = "null")
        List<DayOfWeek> activeWeekdays,
        @Schema(example = "[\"Ricompense doppie in Simulation Training e Forgery Challenge, massimo 3 al giorno\"]")
        List<String> rewards,
        List<Activity> activities,
        @Schema(description = "null se l'evento non ha un effetto", nullable = true, types = {"object", "null"})
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
    @Schema(name = "EventSchedule")
    public record Schedule(
            @Schema(example = "RECURRING") ScheduleType type,
            @Schema(description = "Solo FIXED", nullable = true, example = "null") LocalDate startDate,
            @Schema(description = "Solo FIXED", nullable = true, example = "null") LocalDate endDate,
            @Schema(description = "Solo RECURRING: giorni dalla data di partenza della demo", nullable = true,
                    example = "3")
            Integer startOffsetDays,
            @Schema(description = "Solo RECURRING", nullable = true, example = "7") Integer durationDays,
            @Schema(description = "Solo RECURRING", nullable = true, example = "14") Integer repeatEveryDays) {}
}
