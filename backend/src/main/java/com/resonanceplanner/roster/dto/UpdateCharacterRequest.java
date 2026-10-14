package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.Progress;
import jakarta.validation.constraints.NotNull;

/** New levels and goal of a character that is already in the roster: the character is the one of the URL. */
public record UpdateCharacterRequest(
        @NotNull(message = "Inserisci il livello attuale.") Integer currentLevel,
        @NotNull(message = "Inserisci il grado attuale.") Integer currentRank,
        @NotNull(message = "Inserisci il livello obiettivo.") Integer targetLevel,
        @NotNull(message = "Inserisci il grado obiettivo.") Integer targetRank) {

    public Progress progress() {
        return new Progress(currentLevel, currentRank, targetLevel, targetRank);
    }
}
