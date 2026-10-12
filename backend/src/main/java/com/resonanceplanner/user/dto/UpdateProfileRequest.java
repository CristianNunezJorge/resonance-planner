package com.resonanceplanner.user.dto;

import com.resonanceplanner.user.UsernameRules;
import jakarta.validation.constraints.Pattern;

/**
 * The changes of a profile. A missing field, or one that is null, means "leave it as it is". The play time is checked by the service,
 * because the valid values are in the game rules. Email, role and the other fields of the account cannot be changed
 * here: if the request contains them, they are ignored.
 */
public record UpdateProfileRequest(
        @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.MESSAGE)
        String username,

        Integer playTimeMinutes) {}
