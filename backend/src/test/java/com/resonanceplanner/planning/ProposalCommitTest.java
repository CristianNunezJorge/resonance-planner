package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.planner.PlannerService;
import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleRequest;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
// Today is the real day: the plans start from it, whatever it is.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class ProposalCommitTest {

    private static final int REQUESTS = 6;

    @Autowired
    private ProposalService proposalService;

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesEveryChangeInTheDatabaseWhenEachCallEnds() {
        User user = newUser("proposal.commit");
        try {
            ProposalResponse first = proposalService.createBasic(user.getId());
            assertThat(proposalStatus(first.id())).isEqualTo("PENDING");
            // The fingerprint and the days of the plan are saved with it (D125)
            assertThat(jdbc.queryForObject("select length(inputs_hash) from plan_proposal where id = ?",
                    Integer.class, first.id())).isEqualTo(64);
            assertThat(jdbc.queryForObject("select last_day - first_day from plan_proposal where id = ?",
                    Integer.class, first.id())).isEqualTo(6);
            assertThat(taskCount(first.id(), "PROPOSED")).isEqualTo(first.tasks().size()).isPositive();

            proposalService.confirm(user.getId(), first.id());
            assertThat(proposalStatus(first.id())).isEqualTo("CONFIRMED");
            assertThat(taskCount(first.id(), "CONFIRMED")).isEqualTo(first.tasks().size());

            ProposalResponse second = proposalService.createBasic(user.getId());
            proposalService.reject(user.getId(), second.id());
            assertThat(proposalStatus(second.id())).isEqualTo("REJECTED");
            assertThat(taskCount(second.id(), "PROPOSED")).isZero();

            ProposalResponse third = proposalService.createBasic(user.getId());
            ProposalResponse fourth = proposalService.createBasic(user.getId());
            assertThat(proposalStatus(third.id())).isEqualTo("REPLACED");
            assertThat(taskCount(third.id(), "PROPOSED")).isZero();
            proposalService.confirm(user.getId(), fourth.id());
            // The tasks of the first plan from today on are replaced by the new one (D22)
            assertThat(taskCount(first.id(), "CONFIRMED")).isZero();
            assertThat(taskCount(first.id(), "REPLACED")).isEqualTo(first.tasks().size());
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void savesThePlanTheAgentSawOnlyWhileItsDataStayTheSame() {
        User user = newUser("proposal.agent");
        try {
            ScheduleRequest request = new ScheduleRequest(null, 3, null, null, false, null);
            ScheduleDraft seen = plannerService.draft(user.getId(), request);

            ProposalResponse saved = proposalService.saveIfUnchanged(user.getId(), request, seen, "Piano dell'agente.");

            assertThat(proposalStatus(saved.id())).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select summary from plan_proposal where id = ?", String.class, saved.id()))
                    .isEqualTo("Piano dell'agente.");
            assertThat(taskCount(saved.id(), "PROPOSED"))
                    .isEqualTo(seen.days().stream().mapToInt(day -> day.tasks().size()).sum())
                    .isPositive();

            // The roster changes after the model saw the plan: that plan is not saved (D127)
            jdbc.update("update owned_character set current_level = 10 where user_id = ?", user.getId());

            assertThatThrownBy(() -> proposalService.saveIfUnchanged(user.getId(), request, seen, "Piano vecchio."))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage(ProposalService.DATA_CHANGED);
            assertThat(jdbc.queryForList("select id from plan_proposal where user_id = ? and status = 'PENDING'",
                    Long.class, user.getId())).containsExactly(saved.id());
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void refusesTheBasicPlanAfterTwentyProposalsOfTheDay() {
        User user = newUser("proposal.limit");
        try {
            // Twenty proposals of today, of the basic plan or of the agent: they count together (D129)
            for (int i = 0; i < 19; i++) {
                jdbc.update("insert into plan_proposal (user_id, status, summary, created_at) "
                        + "values (?, 'REPLACED', 'Piano.', now())", user.getId());
            }
            jdbc.update("insert into plan_proposal (user_id, status, summary, created_at) "
                    + "values (?, 'REPLACED', 'Di ieri.', now() - interval '2 days')", user.getId());
            ProposalResponse twentieth = proposalService.createBasic(user.getId());
            assertThat(proposalStatus(twentieth.id())).isEqualTo("PENDING");

            assertThatThrownBy(() -> proposalService.createBasic(user.getId()))
                    .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
                        assertThat(error.getMessage()).isEqualTo(ProposalService.BASIC_PLAN_LIMIT.formatted(20));
                        assertThat(error.retryAfter()).isPositive();
                    });
            // The agent is limited by its messages, not by this count
            ScheduleRequest request = new ScheduleRequest(null, 3, null, null, false, null);
            ScheduleDraft seen = plannerService.draft(user.getId(), request);
            assertThat(proposalService.saveIfUnchanged(user.getId(), request, seen, "Piano dell'agente.").id())
                    .isNotNull();
            assertThat(proposalStatus(twentieth.id())).isEqualTo("REPLACED");
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void leavesOnePendingProposalWhenManyRequestsArriveTogether() throws Exception {
        User user = newUser("proposal.together");
        try {
            List<Throwable> failures = createTogether(user);

            // The lock on the user puts the requests in a row: each one replaces the one before (D37, D124)
            assertThat(failures).isEmpty();
            assertThat(jdbc.queryForObject("select count(*) from plan_proposal where user_id = ? and status = 'PENDING'",
                    Integer.class, user.getId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from plan_proposal where user_id = ? and status = 'REPLACED'",
                    Integer.class, user.getId())).isEqualTo(REQUESTS - 1);
            assertThat(jdbc.queryForObject("""
                    select count(distinct proposal_id) from planned_task where user_id = ? and status = 'PROPOSED'""",
                    Integer.class, user.getId())).isEqualTo(1);
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void confirmsAProposalOnlyOnceWhenManyRequestsArriveTogether() throws Exception {
        User user = newUser("proposal.confirm");
        try {
            ProposalResponse proposal = proposalService.createBasic(user.getId());

            List<Throwable> failures = together(() -> proposalService.confirm(user.getId(), proposal.id()));

            // One request confirms it, the others find it no longer pending
            assertThat(failures).hasSize(REQUESTS - 1).allMatch(ConflictException.class::isInstance);
            assertThat(proposalStatus(proposal.id())).isEqualTo("CONFIRMED");
            assertThat(taskCount(proposal.id(), "CONFIRMED")).isEqualTo(proposal.tasks().size());
        } finally {
            cleanUp(user);
        }
    }

    // Creates the basic plans from as many threads, all released at the same moment; returns what the failed ones threw
    private List<Throwable> createTogether(User user) throws Exception {
        return together(() -> proposalService.createBasic(user.getId()));
    }

    // Runs the call from as many threads, all released at the same moment; returns what the failed ones threw
    private List<Throwable> together(Callable<?> call) throws Exception {
        // The first call is slow (connections, query plans): done before, so the requests really overlap
        User warmUp = newUser("proposal.warm");
        try {
            proposalService.createBasic(warmUp.getId());
        } finally {
            cleanUp(warmUp);
        }
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return call.call();
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

    private String proposalStatus(long proposalId) {
        return jdbc.queryForObject("select status from plan_proposal where id = ?", String.class, proposalId);
    }

    private int taskCount(long proposalId, String taskStatus) {
        return jdbc.queryForObject("select count(*) from planned_task where proposal_id = ? and status = ?",
                Integer.class, proposalId, taskStatus);
    }

    // A name and an email that no earlier run can have left behind, with Jinhsi to plan for
    private User newUser(String prefix) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        User user = User.create(unique, unique + "@example.com", "hash", Role.USER);
        user.changePlayTimeMinutes(60);
        User saved = userRepository.saveAndFlush(user);
        jdbc.update("""
                insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                    target_rank)
                values (?, 'jinhsi', 1, 0, 40, 2)""", saved.getId());
        return saved;
    }

    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
    }
}
