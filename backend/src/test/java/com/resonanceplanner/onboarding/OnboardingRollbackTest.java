package com.resonanceplanner.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

// All or nothing (D50), proved on the real database. The service reads the clock as its very last step, after it
// claimed the setup, deleted the old roster, wrote both characters and changed the user: here the clock fails at that
// moment. Nothing of all that may remain. The test has no @Transactional, so the transaction that rolls back is the
// one of the service.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class OnboardingRollbackTest {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void leavesNothingWhenTheLastStepFailsAfterEverythingWasWritten() {
        String unique = "onboarding.rollback." + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.USER));
        try {
            // A character added before through the API: the setup deletes it, and the rollback must bring it back
            jdbc.update("insert into owned_character (user_id, character_id, current_level, current_rank, target_level, target_rank) "
                    + "values (?, 'yinlin', 1, 0, 20, 0)", user.getId());
            OnboardingRequest request = new OnboardingRequest(
                    List.of(new AddCharacterRequest("jinhsi", 1, 0, 40, 2), new AddCharacterRequest("aalto", 20, 1, 40, 1)),
                    new UpdateWaveplateRequest(180, null), 60);

            // What the database shows from inside the transaction of the service, at the moment the clock fails. The
            // JdbcTemplate shares the connection of that transaction, so it sees the writes that are not committed:
            // this proves that the setup really had been written, and the rollback is not an empty promise.
            AtomicReference<Boolean> claimedInside = new AtomicReference<>();
            AtomicReference<List<String>> rosterInside = new AtomicReference<>();
            doAnswer(invocation -> {
                claimedInside.set(jdbc.queryForObject("select onboarding_completed from users where id = ?", Boolean.class, user.getId()));
                rosterInside.set(jdbc.queryForList("select character_id from owned_character where user_id = ? order by id", String.class, user.getId()));
                throw new IllegalStateException("the clock fails at the last step");
            }).when(clock).instant();

            assertThatThrownBy(() -> onboardingService.complete(user.getId(), request))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(claimedInside.get()).as("the setup was claimed before the failure").isTrue();
            assertThat(rosterInside.get()).as("the new roster was written before the failure").containsExactly("jinhsi", "aalto");
            // Queries of their own, after the end: they see only what was really committed
            assertThat(jdbc.queryForObject("select onboarding_completed from users where id = ?", Boolean.class, user.getId())).isFalse();
            assertThat(jdbc.queryForObject("select play_time_minutes from users where id = ?", Integer.class, user.getId())).isNull();
            assertThat(jdbc.queryForObject("select waveplate_value from users where id = ?", Integer.class, user.getId())).isNull();
            assertThat(jdbc.queryForList("select character_id from owned_character where user_id = ?", String.class, user.getId()))
                    .containsExactly("yinlin");
        } finally {
            // This test commits the user, so it cleans up after itself (ON DELETE CASCADE removes the roster with it)
            jdbc.update("delete from users where id = ?", user.getId());
        }
    }
}
