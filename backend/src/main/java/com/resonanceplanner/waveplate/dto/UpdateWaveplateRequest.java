package com.resonanceplanner.waveplate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * The values that the user reads in the game. The range of each value and the rule between the two are checked by
 * WaveplateRules, which knows the numbers of the game. Missing Crystals mean zero.
 */
public record UpdateWaveplateRequest(
        @Schema(description = "Le Waveplate che vedi nel gioco, da 0 a 240", example = "180")
        @NotNull(message = "Inserisci le Waveplate che vedi nel gioco.") Integer value,
        @Schema(description = "I Crystals, da 0 a 480, solo con le Waveplate piene; assente = 0", example = "0")
        Integer crystals) {

    /** The Crystals of the reading: the ones sent, or 0 if the user did not send any. */
    public int crystalsOrZero() {
        return crystals == null ? 0 : crystals;
    }
}
