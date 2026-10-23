package com.resonanceplanner.planning.dto;

import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.planning.TaskOrigin;
import com.resonanceplanner.planning.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task of the calendar. {@code farmingSourceId} and {@code runs} are set only for FARMING, {@code eventId} only for
 * EVENT; {@code waveplates} is runs × cost of the source, 0 for the other kinds; {@code completedAt} is null until the
 * task is done.
 */
public record TaskResponse(
        @Schema(example = "42") Long id,
        @Schema(description = "AGENT se viene da un piano, MANUAL se l'hai scritta tu", example = "AGENT") TaskOrigin origin,
        @Schema(example = "FARMING") TaskKind kind,
        @Schema(example = "CONFIRMED") TaskStatus status,
        @Schema(example = "2026-10-23") LocalDate taskDate,
        @Schema(example = "Boss Challenge: Mourning Aix") String title,
        @Schema(description = "null se non ci sono", nullable = true, example = "null") String notes,
        @Schema(example = "8") int minutes,
        @Schema(description = "Solo per FARMING", nullable = true, example = "boss-mourning-aix") String farmingSourceId,
        @Schema(description = "Solo per FARMING", nullable = true, example = "2") Integer runs,
        @Schema(description = "Run × costo della fonte; 0 per le altre attività", example = "120") int waveplates,
        @Schema(description = "Solo per EVENT", nullable = true, example = "null") Long eventId,
        @Schema(description = "Il personaggio per cui serve; null se non c'è", nullable = true, example = "jinhsi")
        String characterId,
        @Schema(description = "null finché non è fatta", nullable = true, example = "null") Instant completedAt) {

    public static TaskResponse from(PlannedTask task) {
        FarmingSource source = task.getFarmingSource();
        String sourceId = source == null ? null : source.getId();
        int waveplates = source == null ? 0 : Math.multiplyExact(task.getRuns(), source.getWaveplateCost());
        return new TaskResponse(task.getId(), task.getOrigin(), task.getKind(), task.getStatus(), task.getTaskDate(),
                task.getTitle(), task.getNotes(), task.getMinutes(), sourceId, task.getRuns(), waveplates,
                task.getEventId(), task.getCharacterId(), task.getCompletedAt());
    }
}
