package com.resonanceplanner.planning;

/** Where a proposal is in its life (D22, D124): PENDING until the user confirms or rejects it, or a new one replaces it. */
public enum ProposalStatus {
    PENDING, CONFIRMED, REJECTED, REPLACED
}
