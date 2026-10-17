package com.resonanceplanner.agent.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.planning.dto.TaskResponse;
import java.util.List;
import java.util.Map;

/**
 * What the tools give back to the model (D127). Only what the model needs: never the email or other data of the
 * profile, and the moments in Italian time (D41), like the date of today in the message of the user (D58).
 */
final class ToolResults {

    private ToolResults() {}

    /** {@code waveplates} and {@code playTimeMinutes} are null when not saved yet; {@code notes} say what then counts. */
    record PlayerStatus(List<RosterCharacter> roster, Waveplates waveplates, Integer playTimeMinutes,
            List<String> notes) {}

    record RosterCharacter(String characterId, String name, int currentLevel, int currentRank, int targetLevel,
            int targetRank) {}

    /** The moments are ISO texts with the Italian offset, for example 2026-10-17T22:00:00+02:00. */
    record Waveplates(int value, int max, String fullAt, int crystals, int crystalsMax, String crystalsFullAt) {}

    record Events(List<EventResponse> events) {}

    /** The calendar without its events, which have their own tool; the pending proposal without its tasks. */
    record Calendar(List<TaskResponse> tasks, PendingProposal pendingProposal, boolean planOutdated) {}

    record PendingProposal(Long id, String summary) {}

    record ProposalSaved(Long proposalId, int tasks, String message) {}

    /** A tool that could not run: the message, and the message of every wrong parameter (D61). */
    record Failure(String error, @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> fields) {}
}
