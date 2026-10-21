package com.resonanceplanner.auth;

import com.resonanceplanner.auth.dto.AuthResponse;
import com.resonanceplanner.auth.dto.LoginRequest;
import com.resonanceplanner.auth.dto.RegisterRequest;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.UserService;
import com.resonanceplanner.user.dto.UserResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration and login (D10, D45). It has no @Transactional on purpose: every repository call has its own
 * transaction, so a database connection is never kept busy while BCrypt is computing.
 */
@Service
public class AuthService {

    private static final String EMAIL_TAKEN = "Questa email è già registrata.";

    // Guests of "Prova la demo" (D134): at most 50 at the same time, deleted one hour after another at the latest
    static final int MAX_GUESTS = 50;
    static final String TOO_MANY_GUESTS = "Troppe prove in corso: riprova più tardi o registrati.";
    private static final Duration GUESTS_RETRY_AFTER = Duration.ofHours(1);
    private static final String GUEST_NAME_PREFIX = "ospite-";
    private static final String GUEST_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int GUEST_NAME_LENGTH = 6;
    private static final int GUEST_NAME_ATTEMPTS = 3;
    // A domain reserved for names that can never exist (RFC 2606): the address is never a real mailbox
    private static final String GUEST_EMAIL_DOMAIN = "@ospite.invalid";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // The rules on the username are the same as in the profile
    private final UserService userService;

    // Nobody can register the identity of the admin, even in the seconds before the admin account exists (D100)
    private final String adminEmail;

    // Checked when the email is unknown, so a missing account takes as long as a wrong password
    private final String unknownAccountHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AppProperties appProperties, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.adminEmail = User.normalizeEmail(appProperties.admin().email());
        this.unknownAccountHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        failIfTaken(email, request.username());

        User user = User.create(request.username(), email, passwordEncoder.encode(request.password()), Role.USER);
        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two requests can pass the checks together: the unique indexes of the table decide who wins
            failIfTaken(email, request.username());
            throw e;
        }
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email())).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(
                request.password(), user != null ? user.getPasswordHash() : unknownAccountHash);
        if (user == null || !passwordMatches) {
            // One answer for both cases: it does not tell which of email and password is wrong (F2)
            throw new BadCredentialsException("Bad credentials");
        }
        return toResponse(user);
    }

    /**
     * A temporary account for "Prova la demo" (D134): role GUEST, a random name, an address that cannot exist and the
     * hash of a password nobody knows, so only the token opens it. The cleanup deletes it when the token expires.
     *
     * @throws TooManyRequestsException with RATE_LIMITED if 50 guests already exist
     */
    public AuthResponse guest() {
        // Two requests at the same moment can both pass with 49 guests: one more is harmless
        if (userRepository.countByRole(Role.GUEST) >= MAX_GUESTS) {
            throw new TooManyRequestsException(ErrorCode.RATE_LIMITED, TOO_MANY_GUESTS, GUESTS_RETRY_AFTER);
        }
        String email = UUID.randomUUID() + GUEST_EMAIL_DOMAIN;
        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        User guest = User.create(freeGuestName(), email, password, Role.GUEST);
        return toResponse(userRepository.saveAndFlush(guest));
    }

    // 36^6 names for at most 50 guests: a name already used is almost impossible, but it is checked all the same
    private String freeGuestName() {
        for (int attempt = 0; attempt < GUEST_NAME_ATTEMPTS; attempt++) {
            String name = GUEST_NAME_PREFIX + randomText(GUEST_NAME_LENGTH);
            if (!userRepository.existsByUsername(name)) {
                return name;
            }
        }
        throw new IllegalStateException("No free guest name after " + GUEST_NAME_ATTEMPTS + " attempts");
    }

    private static String randomText(int length) {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(GUEST_NAME_CHARACTERS.charAt(RANDOM.nextInt(GUEST_NAME_CHARACTERS.length())));
        }
        return text.toString();
    }

    private void failIfTaken(String email, String username) {
        if (email.equals(adminEmail) || userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN, "email");
        }
        userService.requireUsernameAvailable(username);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getRole()), UserResponse.from(user));
    }
}
