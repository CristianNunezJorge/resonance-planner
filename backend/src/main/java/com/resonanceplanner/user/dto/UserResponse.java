package com.resonanceplanner.user.dto;

import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** The public data of an account: never the password hash (D47). */
public record UserResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "rover") String username,
        @Schema(example = "rover@example.com") String email,
        @Schema(example = "USER") Role role,
        @Schema(description = "Minuti di gioco al giorno; null prima della configurazione guidata", nullable = true,
                example = "60")
        Integer playTimeMinutes,
        @Schema(example = "true") boolean onboardingCompleted) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getPlayTimeMinutes(), user.isOnboardingCompleted());
    }
}
