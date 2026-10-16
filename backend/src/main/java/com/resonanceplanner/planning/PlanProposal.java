package com.resonanceplanner.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A plan made by the planner that waits for the user (D2, D22): its tasks are PROPOSED until it is confirmed. A user
 * has at most one PENDING proposal, also with two requests at the same moment (D37). The row stays after it is
 * decided, as history (D124).
 */
@Entity
@Table(name = "plan_proposal")
public class PlanProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status;

    @Column(updatable = false)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant decidedAt;

    // The fingerprint of the data of the plan and its days (D125); empty for the proposals saved before V2
    @Column(updatable = false)
    private String inputsHash;

    @Column(updatable = false)
    private LocalDate firstDay;

    @Column(updatable = false)
    private LocalDate lastDay;

    protected PlanProposal() {
        // for JPA
    }

    /** A new proposal for the days from firstDay to lastDay, made from the data with that fingerprint (D125). */
    public static PlanProposal pending(Long userId, String summary, Instant now, LocalDate firstDay,
            LocalDate lastDay, String inputsHash) {
        PlanProposal proposal = new PlanProposal();
        proposal.userId = userId;
        proposal.status = ProposalStatus.PENDING;
        proposal.summary = summary;
        proposal.createdAt = now;
        proposal.firstDay = firstDay;
        proposal.lastDay = lastDay;
        proposal.inputsHash = inputsHash;
        return proposal;
    }

    /** Ends the wait: confirmed, rejected or replaced by a newer proposal. */
    public void decide(ProposalStatus decision, Instant now) {
        if (decision == ProposalStatus.PENDING) {
            throw new IllegalArgumentException("A decision cannot make a proposal pending again");
        }
        this.status = decision;
        this.decidedAt = now;
    }

    public boolean isPending() {
        return status == ProposalStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getInputsHash() {
        return inputsHash;
    }

    public LocalDate getFirstDay() {
        return firstDay;
    }

    public LocalDate getLastDay() {
        return lastDay;
    }
}
