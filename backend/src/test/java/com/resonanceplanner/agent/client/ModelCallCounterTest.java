package com.resonanceplanner.agent.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.testsupport.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// The calls to the model of each user and of the whole app, on the test database (D129). No @Transactional on the
// test: every call commits its own upsert, like in production. The clock is in a year no other test uses, the users
// are only ids (the table has no foreign key, on purpose), and the rows are removed.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class ModelCallCounterTest {

    // 23:30 in Rome is already the next day there, but OpenRouter counts by the day in UTC
    private static final Instant LATE_EVENING = Instant.parse("2031-03-03T22:30:00Z");
    private static final LocalDate UTC_DAY = LocalDate.of(2031, 3, 3);
    private static final Long ROVER = 900_001L;
    private static final Long OTHER = 900_002L;

    @Autowired
    private ModelCallDayRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private final MutableClock clock = new MutableClock(LATE_EVENING);

    @AfterEach
    void removeTheRowsOfTheTest() {
        jdbc.update("delete from agent_model_calls where call_date between ? and ?", UTC_DAY, UTC_DAY.plusDays(1));
    }

    @Test
    void startsFromZeroAndCountsTheCallsOfEachUserAndOfTheWholeApp() {
        ModelCallCounter counter = new ModelCallCounter(repository, clock);
        assertThat(counter.callsToday()).isZero();
        assertThat(counter.callsToday(ROVER)).isZero();

        counter.add(ROVER);
        counter.add(ROVER);
        counter.add(OTHER);

        assertThat(counter.callsToday(ROVER)).isEqualTo(2);
        assertThat(counter.callsToday(OTHER)).isEqualTo(1);
        assertThat(counter.callsToday()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select calls from agent_model_calls where call_date = ? and user_id = ?",
                Integer.class, UTC_DAY, ROVER)).isEqualTo(2);
    }

    // The guests of D134 have a share of their own: the flag is taken from the role in the same statement
    @Test
    void countsTheCallsOfTheGuestsApartAndKeepsThemAfterTheGuestIsDeleted() {
        String username = "ospite-count-" + Long.toString(System.nanoTime() % 100_000_000L, 36);
        Long guest = jdbc.queryForObject("""
                insert into users (username, email, password_hash, role) values (?, ?, 'hash', 'GUEST') returning id""",
                Long.class, username, username + "@ospite.invalid");
        try {
            ModelCallCounter counter = new ModelCallCounter(repository, clock);
            counter.add(guest);
            counter.add(guest);
            counter.add(ROVER);

            assertThat(counter.guestCallsToday()).isEqualTo(2);
            assertThat(counter.callsToday()).isEqualTo(3);

            // No foreign key, on purpose: the cleanup of the guests does not give their calls back
            jdbc.update("delete from users where id = ?", guest);
            assertThat(counter.guestCallsToday()).isEqualTo(2);
            assertThat(counter.callsToday()).isEqualTo(3);
        } finally {
            jdbc.update("delete from users where id = ?", guest);
        }
    }

    @Test
    void startsAgainAtMidnightUtcLikeOpenRouter() {
        ModelCallCounter counter = new ModelCallCounter(repository, clock);
        counter.add(ROVER);
        assertThat(counter.nextReset()).isEqualTo(Instant.parse("2031-03-04T00:00:00Z"));

        clock.advance(Duration.ofHours(2));

        assertThat(counter.callsToday()).isZero();
        assertThat(counter.callsToday(ROVER)).isZero();
        assertThat(counter.nextReset()).isEqualTo(Instant.parse("2031-03-05T00:00:00Z"));
    }

    @Test
    void losesNoCallWhenManyArriveAtTheSameMoment() throws Exception {
        ModelCallCounter counter = new ModelCallCounter(repository, clock);
        int calls = 12;
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            Callable<Void> one = () -> {
                counter.add(ROVER);
                return null;
            };
            for (Future<Void> result : executor.invokeAll(IntStream.range(0, calls).mapToObj(i -> one).toList())) {
                result.get();
            }
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(counter.callsToday(ROVER)).isEqualTo(calls);
        assertThat(counter.callsToday()).isEqualTo(calls);
    }
}
