package com.resonanceplanner.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Integration test on the test database: the entity must match the users table that Flyway created (D38)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsANewUserWithTheDefaultsOfTheSchema() {
        Long id = userRepository.saveAndFlush(User.create("rover", "Rover@Example.COM", "hash", Role.USER)).getId();
        entityManager.clear();

        User reloaded = userRepository.findById(id).orElseThrow();

        assertThat(reloaded.getUsername()).isEqualTo("rover");
        assertThat(reloaded.getEmail()).isEqualTo("rover@example.com");
        assertThat(reloaded.getPasswordHash()).isEqualTo("hash");
        assertThat(reloaded.getRole()).isEqualTo(Role.USER);
        assertThat(reloaded.getPlayTimeMinutes()).isNull();
        assertThat(reloaded.isOnboardingCompleted()).isFalse();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void findsAUserByEmailAndChecksIfAnEmailOrUsernameIsTaken() {
        userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));

        assertThat(userRepository.findByEmail("rover@example.com")).isPresent();
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("rover@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("rover")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    // The registration relies on this exception type to answer 409 when two requests race (AuthService)
    @Test
    void rejectsASecondUserWithTheSameEmailWithADataIntegrityViolation() {
        userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(User.create("other", "rover@example.com", "hash", Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsASecondUserWithTheSameUsernameWithADataIntegrityViolation() {
        userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(User.create("rover", "other@example.com", "hash", Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void normalizesAnEmailToLowercaseWithoutSurroundingSpaces() {
        assertThat(User.normalizeEmail("  Rover@Example.COM ")).isEqualTo("rover@example.com");
    }
}
