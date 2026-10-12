package com.resonanceplanner.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Integration test on the test database: the admin was already created at startup; every test is rolled back
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminSeederTest {

    @Autowired
    private AdminSeeder adminSeeder;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsTheAdminAccountAtStartupFromTheEnvironmentVariables() {
        User admin = userRepository.findByEmail(appProperties.admin().email()).orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(passwordEncoder.matches(appProperties.admin().password(), admin.getPasswordHash())).isTrue();
    }

    @Test
    void createsTheAccountAgainWhenItIsMissing() {
        jdbc.update("DELETE FROM users WHERE email = ?", appProperties.admin().email());

        adminSeeder.seed();
        entityManager.flush();

        assertThat(userRepository.findByEmail(appProperties.admin().email())).hasValueSatisfying(
                admin -> assertThat(admin.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void doesNotDuplicateOrChangeTheAdminWhenItRunsAgain() {
        String hashBefore = userRepository.findByEmail(appProperties.admin().email()).orElseThrow().getPasswordHash();

        adminSeeder.seed();
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class,
                appProperties.admin().email())).isEqualTo(1);
        assertThat(userRepository.findByEmail(appProperties.admin().email()).orElseThrow().getPasswordHash())
                .isEqualTo(hashBefore);
    }

    @Test
    void stopsWithAClearMessageWhenTheAdminUsernameBelongsToAnotherAccount() {
        // Happens in development after changing ADMIN_EMAIL: the old admin still has the username
        jdbc.update("UPDATE users SET email = 'old-admin@example.com' WHERE email = ?", appProperties.admin().email());
        entityManager.clear();

        assertThatThrownBy(() -> adminSeeder.seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(appProperties.admin().email())
                .hasMessageContaining("'admin'");
    }

    @Test
    void doesNotMakeAdminAnAccountThatWasRegisteredWithTheAdminEmail() {
        jdbc.update("UPDATE users SET role = 'USER' WHERE email = ?", appProperties.admin().email());
        entityManager.clear();

        adminSeeder.seed();
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail(appProperties.admin().email()).orElseThrow().getRole()).isEqualTo(Role.USER);
    }
}
