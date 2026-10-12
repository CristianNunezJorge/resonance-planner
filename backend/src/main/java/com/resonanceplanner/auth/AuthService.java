package com.resonanceplanner.auth;

import com.resonanceplanner.auth.dto.AuthResponse;
import com.resonanceplanner.auth.dto.LoginRequest;
import com.resonanceplanner.auth.dto.RegisterRequest;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.dto.UserResponse;
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
    private static final String USERNAME_TAKEN = "Questo username è già in uso.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Nobody can register the identity of the admin, even in the seconds before the admin account exists (D100)
    private final String adminEmail;

    // Checked when the email is unknown, so a missing account takes as long as a wrong password
    private final String unknownAccountHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    private void failIfTaken(String email, String username) {
        if (email.equals(adminEmail) || userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }
        if (User.ADMIN_USERNAME.equalsIgnoreCase(username) || userRepository.existsByUsername(username)) {
            throw new ConflictException(USERNAME_TAKEN);
        }
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getRole()), UserResponse.from(user));
    }
}
