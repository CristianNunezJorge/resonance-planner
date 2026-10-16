package com.resonanceplanner.planner;

import com.resonanceplanner.planning.TaskKind;
import java.time.LocalDate;
import java.util.List;

/**
 * A plan day by day, calculated by the planner (D57, D123): it is not saved, a proposal is made from it (D56).
 *
 * @param characterPriority the order of the characters that the plan followed
 * @param coverage every material that was missing, with how much of it the plan covers
 * @param complete true when the plan covers everything that was missing
 * @param containsEstimates true when a planned source has an estimated drop (D25)
 * @param warnings sentences for the user, in Italian
 */
public record ScheduleDraft(List<String> characterPriority, List<DayPlan> days, List<Coverage> coverage,
        boolean complete, boolean containsEstimates, List<String> warnings) {

    public ScheduleDraft {
        characterPriority = List.copyOf(characterPriority);
        days = List.copyOf(days);
        coverage = List.copyOf(coverage);
        warnings = List.copyOf(warnings);
    }

    /** A day: what was free on it, what the plan uses and its tasks, the event activities first. */
    public record DayPlan(LocalDate date, boolean excluded, int waveplatesAvailable, int minutesAvailable,
            int waveplatesUsed, int minutesUsed, List<DraftTask> tasks) {

        public DayPlan {
            tasks = List.copyOf(tasks);
        }
    }

    /**
     * A task of the plan. FARMING tasks have a source, runs and the runs boosted by an event (D94); EVENT tasks have
     * the event. {@code characterId} is the character the runs were planned for first.
     */
    public record DraftTask(TaskKind kind, String title, int minutes, int waveplates, String farmingSourceId,
            Integer runs, Integer boostedRuns, Long eventId, String characterId) {}

    /** A material, or the EXP line, with what was missing before the plan and what the plan covers. */
    public record Coverage(String materialId, String name, long missing, long covered) {}
}
