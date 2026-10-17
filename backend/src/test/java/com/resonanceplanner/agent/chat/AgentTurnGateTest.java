package com.resonanceplanner.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.testsupport.TestAppProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// One turn at a time per user and at most two in the whole app (D129)
class AgentTurnGateTest {

    private final AgentTurnGate gate =
            new AgentTurnGate(TestAppProperties.withAgentLimits(new AppProperties.AgentLimits(5, 24, 40, 2, 20)));

    @Test
    void refusesASecondTurnOfTheSameUserWhileTheFirstRuns() {
        try (AgentTurnGate.Pass first = gate.enter(1L)) {
            assertThatThrownBy(() -> gate.enter(1L))
                    .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
                        assertThat(error.getMessage()).isEqualTo(AgentTurnGate.USER_BUSY);
                        assertThat(error.retryAfter()).isEqualTo(Duration.ofMinutes(1));
                    });
        }
    }

    @Test
    void refusesATurnWhenTheWholeAppHasTwoRunning() {
        try (AgentTurnGate.Pass first = gate.enter(1L); AgentTurnGate.Pass second = gate.enter(2L)) {
            assertThatThrownBy(() -> gate.enter(3L))
                    .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
                        assertThat(error.getMessage()).isEqualTo(AgentTurnGate.APP_BUSY);
                    });
        }
    }

    @Test
    void letsTheUserInAgainWhenTheTurnEnds() {
        try (AgentTurnGate.Pass first = gate.enter(1L)) {
            assertThat(gate.running()).isEqualTo(1);
        }

        assertThat(gate.running()).isZero();
        assertThatCode(() -> gate.enter(1L).close()).doesNotThrowAnyException();
    }

    @Test
    void anEndedTurnClosedAgainDoesNotFreeTheNextTurnOfTheUser() {
        AgentTurnGate.Pass first = gate.enter(1L);
        first.close();
        AgentTurnGate.Pass second = gate.enter(1L);

        first.close();

        assertThatThrownBy(() -> gate.enter(1L)).isInstanceOf(TooManyRequestsException.class);
        second.close();
    }

    @Test
    @Timeout(30)
    void letsOnlyOneOfManyTurnsOfTheSameUserStartedAtTheSameMoment() throws Exception {
        int requests = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        List<AgentTurnGate.Pass> passes = new ArrayList<>();
        try {
            Callable<AgentTurnGate.Pass> one = () -> {
                start.await();
                try {
                    return gate.enter(1L);
                } catch (TooManyRequestsException e) {
                    return null;
                }
            };
            List<Future<AgentTurnGate.Pass>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(executor.submit(one));
            }
            start.countDown();
            for (Future<AgentTurnGate.Pass> result : results) {
                AgentTurnGate.Pass pass = result.get();
                if (pass != null) {
                    passes.add(pass);
                }
            }
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(passes).hasSize(1);
        passes.forEach(AgentTurnGate.Pass::close);
    }
}
