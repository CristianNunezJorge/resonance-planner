package com.resonanceplanner.planning.dto;

import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.planning.TaskOrigin;
import com.resonanceplanner.planning.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task of the calendar. {@code farmingSourceId} and {@code runs} are set only for FARMING, {@code eventId} only for
 * EVENT; {@code waveplates} is runs × cost of the source, 0 for the other kinds; {@code completedAt} is null until the
 * task is done.
 */
public record TaskResponse(Long id, TaskOrigin origin, TaskKind kind, TaskStatus status, LocalDate taskDate,
        String title, String notes, int minutes, String farmingSourceId, Integer runs, int waveplates, Long eventId,
        String characterId, Instant completedAt) {

    public static TaskResponse from(PlannedTask task) {
        FarmingSource source = task.getFarmingSource();
        String sourceId = source == null ? null : source.getId();
        int waveplates = source == null ? 0 : Math.multiplyExact(task.getRuns(), source.getWaveplateCost());
        return new TaskResponse(task.getId(), task.getOrigin(), task.getKind(), task.getStatus(), task.getTaskDate(),
                task.getTitle(), task.getNotes(), task.getMinutes(), sourceId, task.getRuns(), waveplates,
                task.getEventId(), task.getCharacterId(), task.getCompletedAt());
    }
}
