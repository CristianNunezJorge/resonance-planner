package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.planning.dto.TaskRequest;
import com.resonanceplanner.planning.dto.TaskResponse;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

// No @Transactional on the test: every call of the service opens and commits its own transaction, like in production.
// In the other tests the service joins the transaction of the test, which never commits, so they could not notice a
// write that answers 200 and saves nothing, nor two requests that really run at the same moment.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class TaskCommitTest {

    private static final LocalDate DAY = LocalDate.parse("2030-01-14");
    private static final String BOSS = "boss-mourning-aix";
    private static final String CORE = "elegy-tacet-core";
    private static final int REQUESTS = 6;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesEveryChangeInTheDatabaseWhenEachCallEnds() {
        User user = newUser("task.commit");
        try {
            TaskResponse created = taskService.create(user.getId(), new TaskRequest("Raccolta", DAY, 20, null));
            assertThat(column("title", created.id())).isEqualTo("Raccolta");

            taskService.update(user.getId(), created.id(), new TaskRequest("Boss", DAY, 30, "Nota"));
            assertThat(column("title", created.id())).isEqualTo("Boss");
            assertThat(column("notes", created.id())).isEqualTo("Nota");

            taskService.complete(user.getId(), created.id());
            assertThat(column("status", created.id())).isEqualTo("DONE");

            long farming = farmingTask(user);
            taskService.complete(user.getId(), farming);
            assertThat(column("status", farming)).isEqualTo("DONE");
            assertThat(quantityOf(user)).isEqualTo(2);

            taskService.delete(user.getId(), created.id());
            assertThat(jdbc.queryForObject("select count(*) from planned_task where id = ?", Integer.class, created.id()))
                    .isZero();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void completesATaskOnlyOnceWhenManyRequestsArriveTogether() throws Exception {
        User user = newUser("task.twice");
        try {
            long task = farmingTask(user);

            List<Throwable> failures = completeTogether(user, List.of(task, task, task, task, task, task));

            // One request completes it, the others find it already done: the drops are added once
            assertThat(failures).hasSize(REQUESTS - 1).allMatch(ConflictException.class::isInstance);
            assertThat(quantityOf(user)).isEqualTo(2);
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void sharesTheDailyLimitOfAnEventBetweenTasksCompletedTogether() throws Exception {
        User user = newUser("task.limit");
        GameEvent event = GameEvent.custom("Boss doppi di prova", EventType.DOUBLE_DROP, "Solo per i test.");
        event.scheduleFixed(DAY, DAY);
        event.setActiveWeekdays(Set.of());
        event.setEffect(new EventEffect(EffectType.DROP_MULTIPLIER, FarmingSourceType.BOSS_CHALLENGE, new BigDecimal("2"), 1));
        Long eventId = gameEventRepository.saveAndFlush(event).getId();
        try {
            List<Long> tasks = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                tasks.add(farmingTask(user));
            }

            List<Throwable> failures = completeTogether(user, tasks);

            // Only the first run of the day is doubled: 4 cores, then 2 for each of the other 5 runs
            assertThat(failures).isEmpty();
            assertThat(quantityOf(user)).isEqualTo(4 + 2 * (REQUESTS - 1));
        } finally {
            cleanUp(user);
            jdbc.update("delete from game_event where id = ?", eventId);
        }
    }

    // Completes the tasks from as many threads, all released at the same moment; returns what the failed ones threw
    private List<Throwable> completeTogether(User user, List<Long> tasks) throws Exception {
        // The first call is slow (connections, query plans): done before, so the requests really overlap
        User warmUp = newUser("task.warm");
        try {
            TaskResponse task = taskService.create(warmUp.getId(), new TaskRequest("Riscaldamento", DAY, 5, null));
            taskService.complete(warmUp.getId(), task.id());
        } finally {
            cleanUp(warmUp);
        }
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> results = new ArrayList<>();
            for (Long task : tasks) {
                results.add(pool.submit(() -> {
                    start.await();
                    taskService.complete(user.getId(), task);
                    return null;
                }));
            }
            start.countDown();
            List<Throwable> failures = new ArrayList<>();
            for (Future<?> result : results) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // A farming task of the agent with 1 run of the boss (2 cores on average); proposals arrive with T30, so SQL
    private long farmingTask(User user) {
        Long proposalId = jdbc.queryForObject(
                "insert into plan_proposal (user_id, status) values (?, 'CONFIRMED') returning id", Long.class, user.getId());
        return jdbc.queryForObject("""
                insert into planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes,
                    farming_source_id, runs)
                values (?, ?, 'AGENT', 'FARMING', 'CONFIRMED', ?, 'Boss', 4, ?, 1) returning id""",
                Long.class, user.getId(), proposalId, DAY, BOSS);
    }

    // A name and an email that no earlier run can have left behind in the test database
    private User newUser(String prefix) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.USER));
    }

    // Queries of their own, outside the persistence context: they see only what was really committed
    private String column(String name, long taskId) {
        return jdbc.queryForObject("select " + name + " from planned_task where id = ?", String.class, taskId);
    }

    private Integer quantityOf(User user) {
        return jdbc.queryForObject("select coalesce(sum(quantity), 0) from inventory_item where user_id = ? and material_id = ?",
                Integer.class, user.getId(), CORE);
    }

    // These tests commit, so they clean up after themselves: ON DELETE CASCADE removes tasks, proposals and inventory
    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
    }
}
