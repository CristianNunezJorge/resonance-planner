package com.resonanceplanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Integration test on the Docker Compose test database; every test is rolled back at the end
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreatesEveryTableOfTheDataModel() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "material", "farming_source", "farming_drop", "level_exp", "game_character", "ascension_cost",
                "game_event", "event_active_weekday", "event_activity", "event_reward",
                "users", "owned_character", "inventory_item",
                "plan_proposal", "planned_task", "agent_message");
    }

    @Test
    void deletesEveryRowThatDependsOnAUserTogetherWithTheUser() {
        // D42: with a foreign key to users that does not cascade, deleting an account would fail or leave data behind
        String foreignKeysToUsers = "FROM pg_constraint WHERE contype = 'f' AND confrelid = 'users'::regclass";

        assertThat(jdbc.queryForObject("SELECT count(*) " + foreignKeysToUsers, Integer.class)).isGreaterThanOrEqualTo(5);
        assertThat(jdbc.queryForList("SELECT conrelid::regclass::text || '.' || conname " + foreignKeysToUsers
                + " AND confdeltype <> 'c'", String.class)).isEmpty();
    }

    @Test
    void usesIntegerInsteadOfSmallintSoHibernateCanMapIntegerFields() {
        List<String> smallintColumns = jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND data_type = 'smallint'
                """, String.class);

        assertThat(smallintColumns).as("D98").isEmpty();
    }

    @Test
    void rejectsARecurringEventWithoutDuration() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO game_event (name, type, description, schedule_type, start_offset_days)
                VALUES ('Broken event', 'COMBAT_STAGES', 'No duration', 'RECURRING', 0)
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("game_event_schedule_check");
    }

    @Test
    void rejectsAnExpMaterialWithoutExp() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO material (id, name, category) VALUES ('test-potion', 'Test Potion', 'CHARACTER_EXP')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("material_exp_check");
    }

    @Test
    void rejectsASecondPendingProposalForTheSameUser() {
        long userId = insertUser("alice");
        insertProposal(userId, "PENDING");

        assertThatThrownBy(() -> insertProposal(userId, "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("plan_proposal_one_pending_per_user_idx");
    }

    @Test
    void allowsClosedProposalsAndOnePendingProposalForEachUser() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");

        insertProposal(alice, "CONFIRMED");
        insertProposal(alice, "REPLACED");
        insertProposal(alice, "PENDING");
        insertProposal(bob, "PENDING");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_proposal", Integer.class)).isEqualTo(4);
    }

    @Test
    void rejectsAManualTaskThatIsNotCustom() {
        long userId = insertUser("alice");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO planned_task (user_id, origin, kind, status, task_date, title, minutes)
                VALUES (?, 'MANUAL', 'EVENT', 'CONFIRMED', DATE '2026-10-10', 'Event drill', 15)
                """, userId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("planned_task_origin_rules_check");
    }

    @Test
    void deletesAllTheUserDataTogetherWithTheUser() {
        long userId = insertUser("alice");
        jdbc.update("INSERT INTO material (id, name, category) VALUES ('test-core', 'Test Core', 'ENEMY_DROP')");
        jdbc.update("INSERT INTO inventory_item (user_id, material_id, quantity) VALUES (?, 'test-core', 3)", userId);
        long proposalId = insertProposal(userId, "PENDING");
        jdbc.update("""
                INSERT INTO planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes)
                VALUES (?, ?, 'AGENT', 'CUSTOM', 'PROPOSED', DATE '2026-10-10', 'Daily quests', 20)
                """, userId, proposalId);
        jdbc.update("INSERT INTO agent_message (user_id, role, content, proposal_id) VALUES (?, 'ASSISTANT', 'Plan', ?)",
                userId, proposalId);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        for (String table : List.of("inventory_item", "plan_proposal", "planned_task", "agent_message")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).as(table).isZero();
        }
    }

    private long insertUser(String username) {
        return jdbc.queryForObject("""
                INSERT INTO users (username, email, password_hash, role)
                VALUES (?, ?, 'not-a-real-hash', 'USER') RETURNING id
                """, Long.class, username, username + "@example.com");
    }

    private long insertProposal(long userId, String status) {
        return jdbc.queryForObject("INSERT INTO plan_proposal (user_id, status) VALUES (?, ?) RETURNING id",
                Long.class, userId, status);
    }
}
