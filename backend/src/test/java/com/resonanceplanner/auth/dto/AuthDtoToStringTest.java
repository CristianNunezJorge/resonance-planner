package com.resonanceplanner.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

// Passwords and tokens must not appear if one of these objects is ever printed or logged
class AuthDtoToStringTest {

    @Test
    void hidesThePasswordOfARegistration() {
        String text = new RegisterRequest("rover", "rover@example.com", "S3cret-pass").toString();

        assertThat(text).doesNotContain("S3cret-pass").contains("rover@example.com");
    }

    @Test
    void hidesThePasswordOfALogin() {
        String text = new LoginRequest("rover@example.com", "S3cret-pass").toString();

        assertThat(text).doesNotContain("S3cret-pass").contains("rover@example.com");
    }

    @Test
    void hidesTheTokenOfAnAnswer() {
        UserResponse user = new UserResponse(1L, "rover", "rover@example.com", Role.USER, null, false);

        assertThat(new AuthResponse("secret.jwt.token", user).toString()).doesNotContain("secret.jwt.token");
    }
}
