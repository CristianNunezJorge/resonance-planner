package com.resonanceplanner.user.dto;

import com.resonanceplanner.user.UsernameRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * The changes of a profile. A missing field, or one that is null, means "leave it as it is". The play time is checked by the service,
 * because the valid values are in the game rules. Email, role and the other fields of the account cannot be changed
 * here: if the request contains them, they are ignored.
 */
public record UpdateProfileRequest(
        @Schema(description = "Nuovo username; assente o null = non cambia", example = "rover.new")
        @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.MESSAGE)
        String username,

        @Schema(description = "Minuti di gioco al giorno: 30, 60 o 120; assente o null = non cambia", example = "120")
        Integer playTimeMinutes) {}
