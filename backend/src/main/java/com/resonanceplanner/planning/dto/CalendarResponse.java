package com.resonanceplanner.planning.dto;

import com.resonanceplanner.event.dto.EventResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Everything the calendar shows for a range, in one answer (D51): the editions of the events, as in
 * {@code /api/events}, the tasks of the user in the calendar (CONFIRMED and DONE) and the pending proposal with all its
 * tasks, also outside the range (D68, D124), or null; {@code planOutdated} is true when roster, goals or events of the
 * last plan changed after it was made (D23, D125).
 */
public record CalendarResponse(
        @Schema(description = "Le edizioni degli eventi nell'intervallo, come in /api/events") List<EventResponse> events,
        @Schema(description = "Le tue attività confermate e fatte, per giorno") List<TaskResponse> tasks,
        @Schema(description = "La proposta in attesa con tutte le sue attività, anche fuori dall'intervallo; "
                + "null se non c'è", nullable = true, types = {"object", "null"})
        ProposalResponse pendingProposal,
        @Schema(description = "true se roster, obiettivi o eventi sono cambiati dopo l'ultimo piano", example = "false")
        boolean planOutdated) {

    public CalendarResponse {
        events = List.copyOf(events);
        tasks = List.copyOf(tasks);
    }
}
