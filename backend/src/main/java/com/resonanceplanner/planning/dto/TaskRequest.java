package com.resonanceplanner.planning.dto;

import com.resonanceplanner.planning.PlannedTask;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The form of a manual task (D24, D117): title, day, duration in minutes and optional notes. Origin, kind and status
 * are not in the body: a manual task is always CUSTOM and starts CONFIRMED.
 */
public record TaskRequest(
        @NotBlank(message = "Scrivi un titolo.")
        @Size(max = PlannedTask.MAX_TITLE_LENGTH, message = "Il titolo può avere al massimo " + PlannedTask.MAX_TITLE_LENGTH + " caratteri.")
        String title,

        @NotNull(message = "Scegli il giorno.")
        LocalDate taskDate,

        @NotNull(message = "Indica la durata in minuti.")
        @Min(value = 1, message = "La durata deve essere di almeno 1 minuto.")
        @Max(value = TaskRequest.MAX_MINUTES, message = "La durata non può superare " + TaskRequest.MAX_MINUTES + " minuti, un giorno intero.")
        Integer minutes,

        @Size(max = TaskRequest.MAX_NOTES_LENGTH, message = "Le note possono avere al massimo " + TaskRequest.MAX_NOTES_LENGTH + " caratteri.")
        String notes) {

    public static final int MAX_MINUTES = 1440;
    public static final int MAX_NOTES_LENGTH = 1000;
}
