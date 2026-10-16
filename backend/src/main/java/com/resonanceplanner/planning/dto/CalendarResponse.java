package com.resonanceplanner.planning.dto;

import com.resonanceplanner.event.dto.EventResponse;
import java.util.List;

/**
 * Everything the calendar shows for a range, in one answer (D51): the editions of the events, as in
 * {@code /api/events}, the tasks of the user in the calendar (CONFIRMED and DONE) and the pending proposal with all its
 * tasks, also outside the range (D68, D124), or null; {@code planOutdated} is true when roster, goals or events of the
 * last plan changed after it was made (D23, D125).
 */
public record CalendarResponse(List<EventResponse> events, List<TaskResponse> tasks,
        ProposalResponse pendingProposal, boolean planOutdated) {

    public CalendarResponse {
        events = List.copyOf(events);
        tasks = List.copyOf(tasks);
    }
}
