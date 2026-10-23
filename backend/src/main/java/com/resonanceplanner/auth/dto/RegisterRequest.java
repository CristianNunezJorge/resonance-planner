package com.resonanceplanner.auth.dto;

import com.resonanceplanner.common.validation.MaxUtf8Bytes;
import com.resonanceplanner.user.UsernameRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data of a new account (D100). Each field has one constraint that rejects an empty value, so the message under the
 * field is always the same. The username is a short word: only letters, digits, dot, hyphen and underscore, so it
 * cannot hide spaces, control characters or look-alike invisible characters.
 */
public record RegisterRequest(
        @Schema(description = "Da 3 a 30 caratteri: lettere, numeri, punto, trattino e underscore", example = "rover")
        @NotNull(message = "Inserisci lo username.")
        @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.MESSAGE)
        String username,

        @Schema(example = "rover@example.com")
        @NotBlank(message = "Inserisci l'email.")
        @Email(message = "Inserisci un indirizzo email valido.")
        @Size(max = 254, message = "L'email è troppo lunga.")
        String email,

        @Schema(description = "Da 8 caratteri a 72 byte", example = "password-di-prova")
        @NotNull(message = "Inserisci la password.")
        @Size(min = 8, message = "Deve avere almeno 8 caratteri.")
        @MaxUtf8Bytes(value = 72, message = "La password è troppo lunga (massimo 72 byte).")
        String password) {

    // Hides the password if the object ever ends up in a log
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
