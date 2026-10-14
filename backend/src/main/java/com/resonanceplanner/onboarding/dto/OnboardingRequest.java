package com.resonanceplanner.onboarding.dto;

import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The three steps of the guided setup, sent together at the end (D20, D50): the characters with their levels and
 * goals (the same shape as adding one to the roster), the Waveplate read in the game (the same shape as saving them)
 * and the daily play time. Here a value must only be there: the rules of the game are checked by the service, which
 * answers with every wrong field at once.
 */
public record OnboardingRequest(
        @NotEmpty(message = "Scegli almeno un personaggio.")
        @Size(max = MAX_CHARACTERS, message = "Hai scelto troppi personaggi.")
        List<@NotNull(message = "Scegli un personaggio.") @Valid AddCharacterRequest> characters,

        @NotNull(message = "Inserisci le Waveplate che vedi nel gioco.")
        @Valid
        UpdateWaveplateRequest waveplate,

        @NotNull(message = "Scegli quanto giochi al giorno.")
        Integer playTimeMinutes) {

    /**
     * More than this can only be a mistake or an attack: a character can be chosen only once and the catalog of the
     * demo has six. The limit runs after the list was built; the size of the whole body is limited before, by the
     * limits of Jackson in application.yml.
     */
    public static final int MAX_CHARACTERS = 50;
}
