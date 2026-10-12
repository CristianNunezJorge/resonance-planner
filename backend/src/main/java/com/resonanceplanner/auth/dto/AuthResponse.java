package com.resonanceplanner.auth.dto;

import com.resonanceplanner.user.dto.UserResponse;

/** Answer of registration and login: the token to send in every request, and the account it belongs to. */
public record AuthResponse(String token, UserResponse user) {

    // Hides the token if the object ever ends up in a log
    @Override
    public String toString() {
        return "AuthResponse[token=***, user=" + user + "]";
    }
}
