package com.resonanceplanner.planner;

import java.time.LocalDate;
import java.util.List;

/**
 * The parameters of a plan, the same as the tool {@code plan_schedule} of the agent (D55): every field can be null,
 * and then it takes the value of {@code 06-agente.md}. {@link PlannerService} checks them.
 *
 * @param startDate from today on; null = today
 * @param days from 1 to 14; null = 7
 * @param characterPriority only characters of the roster, each once; the others follow in the order of the roster
 * @param excludedDates the days the user does not play, at most 14; the ones outside the plan are ignored
 * @param includeEventActivities null = true
 * @param maxMinutesPerDay from 15 to 240; null = the play time of the user (D16)
 */
public record ScheduleRequest(LocalDate startDate, Integer days, List<String> characterPriority,
        List<LocalDate> excludedDates, Boolean includeEventActivities, Integer maxMinutesPerDay) {

    /** Every parameter at its default, like the basic plan (D64). */
    public static ScheduleRequest defaults() {
        return new ScheduleRequest(null, null, null, null, null, null);
    }
}
