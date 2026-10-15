package com.resonanceplanner.planning.dto;

import com.resonanceplanner.event.dto.EventResponse;
import java.util.List;

/**
 * Everything the calendar shows for a range, in one answer (D51): the editions of the events, as in
 * {@code /api/events}, and the tasks of the user in the calendar (CONFIRMED and DONE). The pending proposal arrives
 * with the proposals (T30).
 */
public record CalendarResponse(List<EventResponse> events, List<TaskResponse> tasks) {

    public CalendarResponse {
        events = List.copyOf(events);
        tasks = List.copyOf(tasks);
    }
}
