package com.resonanceplanner.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.dto.UserResponse;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: the service opens and commits its own transaction, like in production. In the other
// tests the service joins the transaction of the test, which never commits, so they could not notice a setup that
// answers 200 and saves nothing, nor two submissions that really run at the same moment (a double click).
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class OnboardingCommitTest {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private static OnboardingRequest requestWith(int playTimeMinutes) {
        return new OnboardingRequest(
                List.of(new AddCharacterRequest("jinhsi", 1, 0, 40, 2), new AddCharacterRequest("aalto", 20, 1, 40, 1)),
                new UpdateWaveplateRequest(180, null), playTimeMinutes);
    }

    @Test
    void writesTheWholeSetupToTheDatabaseWhenTheCallEnds() {
        User user = newUser("onboarding.commit");
        try {
            UserResponse answer = onboardingService.complete(user.getId(), requestWith(60));

            assertThat(answer.onboardingCompleted()).isTrue();
            // Queries of their own, outside the persistence context: they see only what was really committed
            assertThat(jdbc.queryForObject("select onboarding_completed from users where id = ?", Boolean.class, user.getId())).isTrue();
            assertThat(jdbc.queryForObject("select play_time_minutes from users where id = ?", Integer.class, user.getId())).isEqualTo(60);
            assertThat(jdbc.queryForObject("select waveplate_value from users where id = ?", Integer.class, user.getId())).isEqualTo(180);
            assertThat(jdbc.queryForObject("select count(*) from owned_character where user_id = ?", Integer.class, user.getId())).isEqualTo(2);
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void letsOnlyOneOfManySubmissionsAtTheSameMomentWin() throws Exception {
        User user = newUser("onboarding.race");
        int[] playTimes = {30, 60, 120, 30, 60, 120};
        int submissions = playTimes.length;
        ExecutorService pool = Executors.newFixedThreadPool(submissions);
        try {
            // The first call is slow (connections, query plans): done before, so the submissions really overlap
            User warmUp = newUser("onboarding.warm");
            try {
                onboardingService.complete(warmUp.getId(), requestWith(30));
            } finally {
                cleanUp(warmUp);
            }
            CountDownLatch start = new CountDownLatch(1);
            List<Future<UserResponse>> results = new ArrayList<>();
            for (int i = 0; i < submissions; i++) {
                int playTime = playTimes[i];
                results.add(pool.submit(() -> {
                    start.await();
                    return onboardingService.complete(user.getId(), requestWith(playTime));
                }));
            }
            start.countDown();

            List<UserResponse> winners = new ArrayList<>();
            for (Future<UserResponse> result : results) {
                try {
                    winners.add(result.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    // Every other submission must be refused as a conflict, never as a database error
                    assertThat(e.getCause()).isInstanceOf(ConflictException.class);
                }
            }

            assertThat(winners).hasSize(1);
            // Exactly the roster of the one that won: the others never reached the point of writing
            assertThat(jdbc.queryForObject("select count(*) from owned_character where user_id = ?", Integer.class, user.getId())).isEqualTo(2);
            assertThat(jdbc.queryForObject("select play_time_minutes from users where id = ?", Integer.class, user.getId()))
                    .isEqualTo(winners.getFirst().playTimeMinutes());
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            cleanUp(user);
        }
    }

    @Test
    void refusesASecondSetupWithAConflictAfterTheFirstEnded() {
        User user = newUser("onboarding.twice");
        try {
            onboardingService.complete(user.getId(), requestWith(60));

            assertThatThrownBy(() -> onboardingService.complete(user.getId(), requestWith(30)))
                    .isInstanceOf(ConflictException.class);
            assertThat(jdbc.queryForObject("select play_time_minutes from users where id = ?", Integer.class, user.getId())).isEqualTo(60);
        } finally {
            cleanUp(user);
        }
    }

    // A name and an email that no earlier run can have left behind in the test database
    private User newUser(String prefix) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.USER));
    }

    // These tests commit, so they clean up after themselves (ON DELETE CASCADE removes the roster with the user)
    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
    }
}
