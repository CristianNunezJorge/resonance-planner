package com.resonanceplanner.planning;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** The proposals of a user: like the tasks, every query takes the user (D44). */
public interface PlanProposalRepository extends Repository<PlanProposal, Long> {

    PlanProposal saveAndFlush(PlanProposal proposal);

    Optional<PlanProposal> findByIdAndUserId(Long id, Long userId);

    Optional<PlanProposal> findByUserIdAndStatus(Long userId, ProposalStatus status);

    /** The newest proposal with one of the statuses: the last plan, pending or confirmed (D125). */
    Optional<PlanProposal> findFirstByUserIdAndStatusInOrderByIdDesc(Long userId, Collection<ProposalStatus> statuses);
}
