package com.resonanceplanner.waveplate.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The values that the user reads in the game. The range of each value and the rule between the two are checked by
 * WaveplateRules, which knows the numbers of the game. Missing Crystals mean zero.
 */
public record UpdateWaveplateRequest(
        @NotNull(message = "Inserisci le Waveplate che vedi nel gioco.") Integer value,
        Integer crystals) {

    /** The Crystals of the reading: the ones sent, or 0 if the user did not send any. */
    public int crystalsOrZero() {
        return crystals == null ? 0 : crystals;
    }
}
