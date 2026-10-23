package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.Progress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** New levels and goal of a character that is already in the roster: the character is the one of the URL. */
public record UpdateCharacterRequest(
        @Schema(description = "Da 1 a 40", example = "20")
        @NotNull(message = "Inserisci il livello attuale.") Integer currentLevel,
        @Schema(description = "Ascensioni fatte: 0, 1 (al livello 20) o 2 (al 40)", example = "1")
        @NotNull(message = "Inserisci il grado attuale.") Integer currentRank,
        @Schema(example = "40") @NotNull(message = "Inserisci il livello obiettivo.") Integer targetLevel,
        @Schema(example = "2") @NotNull(message = "Inserisci il grado obiettivo.") Integer targetRank) {

    public Progress progress() {
        return new Progress(currentLevel, currentRank, targetLevel, targetRank);
    }
}
