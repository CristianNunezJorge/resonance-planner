package com.resonanceplanner.agent.tools;

import java.time.LocalDate;

/**
 * The arguments the model can write for the tools (D127), read strictly: any other key is an error, so no tool
 * accepts a user (D55). The parameters of {@code plan_schedule} are {@link com.resonanceplanner.planner.ScheduleRequest}.
 */
final class ToolArguments {

    private ToolArguments() {}

    /** {@code get_player_status} has no parameters. */
    record None() {}

    /** {@code get_events} and {@code get_calendar}: both dates can be left out. */
    record Range(LocalDate from, LocalDate to) {}

    /** {@code create_plan_proposal}: the explanation of the plan, written by the model for the user (D56). */
    record Proposal(String summary) {}
}
