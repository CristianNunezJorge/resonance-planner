package com.resonanceplanner.auth.dto;

import com.resonanceplanner.common.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Credentials of a login. Only presence is checked: the format rules belong to the registration.
 * The password is {@code @NotEmpty} and not {@code @NotBlank}, because a password made of spaces is allowed there.
 */
public record LoginRequest(
        @Schema(example = "rover@example.com")
        @NotBlank(message = "Inserisci l'email.")
        String email,

        @Schema(example = "password-di-prova")
        @NotEmpty(message = "Inserisci la password.")
        @MaxUtf8Bytes(value = 72, message = "La password è troppo lunga (massimo 72 byte).")
        String password) {

    // Hides the password if the object ever ends up in a log
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
