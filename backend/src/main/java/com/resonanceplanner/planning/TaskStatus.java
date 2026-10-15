package com.resonanceplanner.planning;

/**
 * Where a task is in its life (docs/02-utenti-e-flussi.md): PROPOSED inside a proposal, CONFIRMED in the calendar,
 * DONE once marked as done; REJECTED and REPLACED are plans of the agent that the user discarded or replaced (D22).
 */
public enum TaskStatus {
    PROPOSED, CONFIRMED, DONE, REJECTED, REPLACED
}
