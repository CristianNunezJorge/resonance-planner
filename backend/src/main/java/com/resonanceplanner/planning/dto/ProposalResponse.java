package com.resonanceplanner.planning.dto;

import com.resonanceplanner.planning.PlanProposal;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.ProposalStatus;
import java.time.Instant;
import java.util.List;

/**
 * A proposal with its tasks (D68, D124), in the same form as the tasks of the calendar. A rejected or replaced
 * proposal has no tasks: they are deleted.
 */
public record ProposalResponse(Long id, ProposalStatus status, String summary, Instant createdAt,
        List<TaskResponse> tasks) {

    public ProposalResponse {
        tasks = List.copyOf(tasks);
    }

    public static ProposalResponse from(PlanProposal proposal, List<PlannedTask> tasks) {
        return new ProposalResponse(proposal.getId(), proposal.getStatus(), proposal.getSummary(),
                proposal.getCreatedAt(), tasks.stream().map(TaskResponse::from).toList());
    }
}
