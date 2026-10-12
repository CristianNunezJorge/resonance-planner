package com.resonanceplanner.auth.dto;

import com.resonanceplanner.common.validation.MaxUtf8Bytes;
import com.resonanceplanner.user.UsernameRules;
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
        @NotNull(message = "Inserisci lo username.")
        @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.MESSAGE)
        String username,

        @NotBlank(message = "Inserisci l'email.")
        @Email(message = "Inserisci un indirizzo email valido.")
        @Size(max = 254, message = "L'email è troppo lunga.")
        String email,

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
