package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.Progress;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Identificativo del catalogo", example = "jinhsi")
        @NotBlank(message = "Scegli un personaggio.")
        @Size(max = 60, message = "Questo personaggio non esiste.")
        @Pattern(regexp = "[a-z0-9-]*", message = "Questo personaggio non esiste.") String characterId,
        @Schema(description = "Da 1 a 40", example = "1")
        @NotNull(message = "Inserisci il livello attuale.") Integer currentLevel,
        @Schema(description = "Ascensioni fatte: 0, 1 (al livello 20) o 2 (al 40)", example = "0")
        @NotNull(message = "Inserisci il grado attuale.") Integer currentRank,
        @Schema(example = "40") @NotNull(message = "Inserisci il livello obiettivo.") Integer targetLevel,
        @Schema(example = "2") @NotNull(message = "Inserisci il grado obiettivo.") Integer targetRank) {

    public Progress progress() {
        return new Progress(currentLevel, currentRank, targetLevel, targetRank);
    }
}
