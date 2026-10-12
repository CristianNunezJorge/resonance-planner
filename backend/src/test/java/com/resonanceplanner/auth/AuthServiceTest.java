package com.resonanceplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resonanceplanner.auth.dto.AuthResponse;
import com.resonanceplanner.auth.dto.LoginRequest;
import com.resonanceplanner.auth.dto.RegisterRequest;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

// Unit test with mocks: the cases that are hard to provoke with a real database (races, timing)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String UNKNOWN_ACCOUNT_HASH = "hash-for-unknown-accounts";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // The service builds the hash for unknown accounts when it is created: it must be a real value in these tests
        when(passwordEncoder.encode(anyString())).thenReturn(UNKNOWN_ACCOUNT_HASH);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, appProperties());
    }

    private static AppProperties appProperties() {
        return new AppProperties(
                new AppProperties.Database("jdbc:postgresql://localhost:5433/test", "user", "password"),
                new AppProperties.Jwt("0123456789abcdef0123456789abcdef"),
                new AppProperties.Admin("Admin@Example.com", "admin-password"),
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.RateLimit(10, Duration.ofMinutes(15), 120, Duration.ofMinutes(1)));
    }

    @Test
    void registersAUserWithTheHashedPasswordAndALowercaseEmail() {
        when(passwordEncoder.encode("S3cret-pass")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any(), any())).thenReturn("the-token");

        AuthResponse response = authService.register(new RegisterRequest("rover", "Rover@Example.COM", "S3cret-pass"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("rover@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.token()).isEqualTo("the-token");
        assertThat(response.user().username()).isEqualTo("rover");
    }

    @Test
    void refusesAnEmailThatIsAlreadyRegistered() {
        when(userRepository.existsByEmail("rover@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("rover", "ROVER@example.com", "S3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questa email è già registrata.");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesTheEmailOfTheAdminEvenBeforeTheAdminAccountExists() {
        // existsByEmail is not stubbed: the database knows nothing yet, the answer comes from ADMIN_EMAIL
        assertThatThrownBy(() -> authService.register(new RegisterRequest("rover", "ADMIN@example.com", "S3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questa email è già registrata.");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesTheUsernameOfTheAdminInAnyLetterCase() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("AdMiN", "rover@example.com", "S3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questo username è già in uso.");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesAUsernameThatIsAlreadyInUse() {
        when(userRepository.existsByUsername("rover")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("rover", "rover@example.com", "S3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questo username è già in uso.");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void turnsARaceOnTheUniqueIndexIntoTheSameConflictAnswer() {
        // Both checks passed, but another request saved the same email a moment before this one
        when(userRepository.existsByEmail("rover@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode("S3cret-pass")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("rover", "rover@example.com", "S3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questa email è già registrata.");
    }

    @Test
    void doesNotHideADatabaseErrorThatIsNotADuplicate() {
        when(passwordEncoder.encode("S3cret-pass")).thenReturn("hashed-password");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("not null violated");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("rover", "rover@example.com", "S3cret-pass")))
                .isSameAs(failure);
    }

    @Test
    void logsInWithTheRightPassword() {
        User user = User.create("rover", "rover@example.com", "stored-hash", Role.USER);
        when(userRepository.findByEmail("rover@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("S3cret-pass", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("the-token");

        AuthResponse response = authService.login(new LoginRequest("Rover@Example.com", "S3cret-pass"));

        assertThat(response.token()).isEqualTo("the-token");
        assertThat(response.user().email()).isEqualTo("rover@example.com");
    }

    @Test
    void refusesAWrongPasswordWithBadCredentials() {
        User user = User.create("rover", "rover@example.com", "stored-hash", Role.USER);
        when(userRepository.findByEmail("rover@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("rover@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void checksAHashEvenForAnUnknownEmailSoTheAnswerTakesTheSameTime() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "S3cret-pass")))
                .isInstanceOf(BadCredentialsException.class);
        // With a null or empty hash the real encoder would answer at once, without running BCrypt
        verify(passwordEncoder).matches("S3cret-pass", UNKNOWN_ACCOUNT_HASH);
    }
}
