package com.resonanceplanner.planning;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** The proposals of a user: like the tasks, every query takes the user (D44). */
public interface PlanProposalRepository extends Repository<PlanProposal, Long> {

    PlanProposal saveAndFlush(PlanProposal proposal);

    Optional<PlanProposal> findByIdAndUserId(Long id, Long userId);

    Optional<PlanProposal> findByUserIdAndStatus(Long userId, ProposalStatus status);
}
