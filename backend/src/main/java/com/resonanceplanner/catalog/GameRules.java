package com.resonanceplanner.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Game rules read from game-rules.json at startup (D33): a few constants that never change
 * while the app runs, so they stay in memory instead of the database.
 */
public record GameRules(
        @Valid @NotNull Regeneration waveplate,
        @Valid @NotNull Regeneration waveplateCrystals,
        @NotEmpty List<Integer> ascensionLevels,
        @Positive int maxLevel,
        @NotNull @Positive BigDecimal shellCreditPerExp,
        @Positive int sol3Phase,
        @NotNull LocalDate demoEventsAnchorDate,
        @NotEmpty List<Integer> playTimeOptionsMinutes) {

    // Read-only copies; a missing list stays null so that the validation can report it
    public GameRules {
        ascensionLevels = ascensionLevels == null ? null : List.copyOf(ascensionLevels);
        playTimeOptionsMinutes = playTimeOptionsMinutes == null ? null : List.copyOf(playTimeOptionsMinutes);
    }

    /** Maximum and regeneration time of Waveplate or Waveplate Crystals (D28). */
    public record Regeneration(@Positive int max, @Positive int regenMinutes) {}
}
