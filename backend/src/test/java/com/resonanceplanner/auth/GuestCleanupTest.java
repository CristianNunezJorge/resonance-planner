package com.resonanceplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// The guests of D134 are deleted when their token has expired, 24 hours after they were created, with all their data
// (D42). The ages are written with SQL; every test is rolled back, and it looks only at its own rows.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuestCleanupTest {

    @Autowired
    private GuestCleanup cleanup;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deletesTheGuestsWhoseTokenHasExpiredWithAllTheirData() {
        long expiredGuest = user("ospite-vecchio", "GUEST", "25 hours");
        long youngGuest = user("ospite-giovane", "GUEST", "23 hours");
        long oldUser = user("rover", "USER", "30 days");
        jdbc.update("INSERT INTO inventory_item (user_id, material_id, quantity) VALUES (?, 'shell-credit', 10)",
                expiredGuest);

        int deleted = cleanup.deleteExpiredGuests();

        assertThat(deleted).isPositive();
        assertThat(exists(expiredGuest)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_item WHERE user_id = ?", Long.class,
                expiredGuest)).isZero();
        // A guest can still use the app until its token expires; registered users are never touched
        assertThat(exists(youngGuest)).isTrue();
        assertThat(exists(oldUser)).isTrue();
    }

    @Test
    void doesTheSameWhenTheScheduleRunsIt() {
        long expiredGuest = user("ospite-scaduto", "GUEST", "2 days");
        long youngGuest = user("ospite-appena", "GUEST", "1 minute");

        cleanup.run();

        assertThat(exists(expiredGuest)).isFalse();
        assertThat(exists(youngGuest)).isTrue();
    }

    private long user(String username, String role, String age) {
        return jdbc.queryForObject("""
                INSERT INTO users (username, email, password_hash, role, created_at)
                VALUES (?, ?, 'hash', ?, now() - ?::interval) RETURNING id""",
                Long.class, username, username + "@example.com", role, age);
    }

    private boolean exists(long id) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Long.class, id) == 1;
    }
}
