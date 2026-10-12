package com.resonanceplanner.user.dto;

import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;

/** The public data of an account: never the password hash (D47). */
public record UserResponse(Long id, String username, String email, Role role, Integer playTimeMinutes,
        boolean onboardingCompleted) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getPlayTimeMinutes(), user.isOnboardingCompleted());
    }
}
