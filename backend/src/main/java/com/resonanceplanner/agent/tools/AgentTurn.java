package com.resonanceplanner.agent.tools;

import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleRequest;
import com.resonanceplanner.planning.dto.ProposalResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One turn of the agent: who asks, and what the tools did so far (D56, D62, D127). The user comes from the token
 * (D44), never from the model. A turn lives for one message of the chat, in one thread, so it is not thread-safe.
 */
public final class AgentTurn {

    private final Long userId;
    private PlannedDraft lastPlan;
    private ProposalResponse proposal;
    private boolean proposalIsLastPlan;
    private final Set<String> steps = new LinkedHashSet<>();

    public AgentTurn(Long userId) {
        this.userId = Objects.requireNonNull(userId, "userId");
    }

    public Long userId() {
        return userId;
    }

    /** The last plan of {@code plan_schedule} in this turn, with its parameters: the only one that can be saved. */
    public Optional<PlannedDraft> lastPlan() {
        return Optional.ofNullable(lastPlan);
    }

    /** The proposal saved in this turn, if any: the answer of the chat shows it (D52). */
    public Optional<ProposalResponse> proposal() {
        return Optional.ofNullable(proposal);
    }

    /** What the agent did, in order and each once, for the answer of the chat (D62). */
    public List<String> steps() {
        return List.copyOf(steps);
    }

    /**
     * False when, after saving the proposal, the model calculated another plan and did not save it: its answer may
     * describe that plan, while the calendar shows the proposal (D128).
     */
    public boolean proposalIsLastPlan() {
        return proposalIsLastPlan;
    }

    void planned(ScheduleRequest request, ScheduleDraft draft) {
        lastPlan = new PlannedDraft(request, draft);
        proposalIsLastPlan = false;
    }

    void forgetPlan() {
        lastPlan = null;
    }

    void proposalSaved(ProposalResponse saved) {
        proposal = Objects.requireNonNull(saved, "saved");
        proposalIsLastPlan = true;
    }

    void stepDone(String step) {
        steps.add(Objects.requireNonNull(step, "step"));
    }

    /** A plan of the planner and the parameters it was calculated with. */
    public record PlannedDraft(ScheduleRequest request, ScheduleDraft draft) {

        public PlannedDraft {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(draft, "draft");
        }
    }
}
