package com.resonanceplanner.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentTurnTest {

    private static final ScheduleDraft EMPTY_DRAFT =
            new ScheduleDraft(List.of(), List.of(), List.of(), true, false, List.of());

    @Test
    void startsWithoutPlanProposalOrSteps() {
        AgentTurn turn = new AgentTurn(7L);

        assertThat(turn.userId()).isEqualTo(7L);
        assertThat(turn.lastPlan()).isEmpty();
        assertThat(turn.proposal()).isEmpty();
        assertThat(turn.steps()).isEmpty();
    }

    @Test
    void needsTheUserOfTheToken() {
        assertThatThrownBy(() -> new AgentTurn(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void remembersOnlyTheLastPlanOfTheTurn() {
        AgentTurn turn = new AgentTurn(7L);
        ScheduleRequest first = new ScheduleRequest(null, 3, null, null, null, null);
        ScheduleRequest second = new ScheduleRequest(null, 5, null, null, null, null);

        turn.planned(first, EMPTY_DRAFT);
        turn.planned(second, EMPTY_DRAFT);

        assertThat(turn.lastPlan()).hasValueSatisfying(plan -> {
            assertThat(plan.request()).isEqualTo(second);
            assertThat(plan.draft()).isEqualTo(EMPTY_DRAFT);
        });
    }

    @Test
    void keepsEveryStepOnceInTheOrderTheyHappened() {
        AgentTurn turn = new AgentTurn(7L);

        turn.stepDone("Ho controllato gli eventi");
        turn.stepDone("Ho calcolato il piano");
        turn.stepDone("Ho controllato gli eventi");

        assertThat(turn.steps()).containsExactly("Ho controllato gli eventi", "Ho calcolato il piano");
        assertThatThrownBy(() -> turn.steps().add("Altro")).isInstanceOf(UnsupportedOperationException.class);
    }
}
