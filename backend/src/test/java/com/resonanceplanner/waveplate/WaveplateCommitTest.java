package com.resonanceplanner.waveplate;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: the service opens and commits its own transaction, like in production. In the other
// tests the service joins the transaction of the test, which never commits, so they could not notice a PUT that
// answers 200 and writes nothing.
@SpringBootTest
@ActiveProfiles("test")
class WaveplateCommitTest {

    @Autowired
    private WaveplateService waveplateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesTheReadingToTheDatabaseWhenTheUpdateEnds() {
        User user = userRepository.saveAndFlush(User.create("commit.check", "commit.check@example.com", "hash", Role.USER));
        try {
            waveplateService.update(user.getId(), new UpdateWaveplateRequest(150, null));

            // A query of its own, outside the persistence context: it sees only what was really committed
            Integer saved = jdbc.queryForObject("select waveplate_value from users where id = ?", Integer.class, user.getId());
            assertThat(saved).isEqualTo(150);
            assertThat(waveplateService.get(user.getId()).value()).isGreaterThanOrEqualTo(150);
        } finally {
            // This test commits, so it cleans up after itself (ON DELETE CASCADE removes whatever depends on the user)
            jdbc.update("delete from users where id = ?", user.getId());
        }
    }
}
