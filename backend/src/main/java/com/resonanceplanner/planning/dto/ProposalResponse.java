package com.resonanceplanner.planning.dto;

import com.resonanceplanner.planning.PlanProposal;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * A proposal with its tasks (D68, D124), in the same form as the tasks of the calendar. A rejected or replaced
 * proposal has no tasks: they are deleted.
 */
public record ProposalResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "PENDING") ProposalStatus status,
        @Schema(description = "La spiegazione del piano, da mostrare come testo semplice",
                example = "Piano per la settimana dal 22 al 28 ottobre: oggi i drill del Lament Recon e le run doppie di "
                        + "Simulation Training per Jinhsi, domani i boss.")
        String summary,
        @Schema(example = "2026-10-22T14:05:00Z") Instant createdAt,
        @Schema(description = "Le attività PROPOSED; vuoto per una proposta scartata o sostituita")
        List<TaskResponse> tasks) {

    public ProposalResponse {
        tasks = List.copyOf(tasks);
    }

    public static ProposalResponse from(PlanProposal proposal, List<PlannedTask> tasks) {
        return new ProposalResponse(proposal.getId(), proposal.getStatus(), proposal.getSummary(),
                proposal.getCreatedAt(), tasks.stream().map(TaskResponse::from).toList());
    }
}
