package com.resonanceplanner.auth.dto;

import com.resonanceplanner.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** Answer of registration and login: the token to send in every request, and the account it belongs to. */
public record AuthResponse(
        @Schema(description = "Da mandare nell'header Authorization: Bearer; vale 24 ore",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZSI6IlVTRVIifQ.firma-di-esempio") String token,
        UserResponse user) {

    // Hides the token if the object ever ends up in a log
    @Override
    public String toString() {
        return "AuthResponse[token=***, user=" + user + "]";
    }
}
