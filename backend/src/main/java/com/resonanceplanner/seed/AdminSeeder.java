package com.resonanceplanner.seed;

import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the admin account from ADMIN_EMAIL and ADMIN_PASSWORD, only if it does not exist yet (D19, D39).
 * An existing account is never changed, so a password changed later survives the restarts of the free hosting.
 */
@Component
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AppProperties appProperties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void seed() {
        String email = User.normalizeEmail(appProperties.admin().email());
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN) {
                // Making it admin would give the control to whoever registered that email first
                log.warn("The account {} exists but is not an admin: it was left unchanged", email);
            }
            return;
        }
        if (userRepository.existsByUsername(User.ADMIN_USERNAME)) {
            throw new IllegalStateException("Cannot create the admin account for " + email
                    + ": the username '" + User.ADMIN_USERNAME + "' belongs to another account");
        }
        userRepository.save(User.create(User.ADMIN_USERNAME, email,
                passwordEncoder.encode(appProperties.admin().password()), Role.ADMIN));
        log.info("Admin account created for {}", email);
    }
}
