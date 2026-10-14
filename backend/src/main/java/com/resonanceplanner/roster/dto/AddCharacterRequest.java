package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.Progress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A character that the user adds to the roster. The ranges of level and rank are checked by RosterRules, which knows
 * the numbers of the game: here a value must only be there. The identifier must look like those of the catalog (lowercase
 * letters, digits and hyphens): any other text would only reach the database to be refused there.
 */
public record AddCharacterRequest(
        @NotBlank(message = "Scegli un personaggio.")
        @Size(max = 60, message = "Questo personaggio non esiste.")
        @Pattern(regexp = "[a-z0-9-]*", message = "Questo personaggio non esiste.") String characterId,
        @NotNull(message = "Inserisci il livello attuale.") Integer currentLevel,
        @NotNull(message = "Inserisci il grado attuale.") Integer currentRank,
        @NotNull(message = "Inserisci il livello obiettivo.") Integer targetLevel,
        @NotNull(message = "Inserisci il grado obiettivo.") Integer targetRank) {

    public Progress progress() {
        return new Progress(currentLevel, currentRank, targetLevel, targetRank);
    }
}
