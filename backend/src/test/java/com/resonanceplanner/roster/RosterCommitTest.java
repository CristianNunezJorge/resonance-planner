package com.resonanceplanner.roster;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.roster.dto.UpdateCharacterRequest;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: every call of the service opens and commits its own transaction, like in production.
// In the other tests the service joins the transaction of the test, which never commits, so they could not notice a
// write that answers 200 and saves nothing.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class RosterCommitTest {

    @Autowired
    private RosterService rosterService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesAddUpdateAndDeleteToTheDatabaseWhenEachCallEnds() {
        // A name and an email that no earlier run can have left behind in the test database
        String unique = "roster.commit." + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.USER));
        try {
            rosterService.add(user.getId(), new AddCharacterRequest("jinhsi", 1, 0, 40, 2));
            assertThat(levelOf(user)).isEqualTo(1);

            rosterService.update(user.getId(), "jinhsi", new UpdateCharacterRequest(20, 1, 40, 2));
            assertThat(levelOf(user)).isEqualTo(20);

            rosterService.delete(user.getId(), "jinhsi");
            assertThat(jdbc.queryForObject("select count(*) from owned_character where user_id = ?", Integer.class, user.getId()))
                    .isZero();
        } finally {
            // This test commits, so it cleans up after itself (ON DELETE CASCADE removes the roster with the user)
            jdbc.update("delete from users where id = ?", user.getId());
        }
    }

    // A query of its own, outside the persistence context: it sees only what was really committed
    private Integer levelOf(User user) {
        return jdbc.queryForObject("select current_level from owned_character where user_id = ? and character_id = 'jinhsi'",
                Integer.class, user.getId());
    }
}
