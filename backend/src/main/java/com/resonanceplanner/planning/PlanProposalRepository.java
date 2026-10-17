package com.resonanceplanner.planning;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** The proposals of a user: like the tasks, every query takes the user (D44). */
public interface PlanProposalRepository extends Repository<PlanProposal, Long> {

    PlanProposal saveAndFlush(PlanProposal proposal);

    Optional<PlanProposal> findByIdAndUserId(Long id, Long userId);

    Optional<PlanProposal> findByUserIdAndStatus(Long userId, ProposalStatus status);

    /** The proposals the user received since a moment, of the basic plan and of the agent (D129). */
    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant since);

    /** The newest proposal with one of the statuses: the last plan, pending or confirmed (D125). */
    Optional<PlanProposal> findFirstByUserIdAndStatusInOrderByIdDesc(Long userId, Collection<ProposalStatus> statuses);
}
