package com.resonanceplanner.planner;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.planner.ScheduleDraft.Coverage;
import com.resonanceplanner.planner.ScheduleDraft.DayPlan;
import com.resonanceplanner.planner.ScheduleDraft.DraftTask;
import com.resonanceplanner.planning.TaskKind;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** When two drafts are the same plan, for the proposal of the agent (D127). */
class ScheduleDraftTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 17);
    private static final DraftTask FORGERY =
            new DraftTask(TaskKind.FARMING, "Farming: Forgery", 30, 80, "forgery-1", 2, 0, null, "jinhsi");
    private static final DraftTask BOSS =
            new DraftTask(TaskKind.FARMING, "Farming: Boss", 20, 60, "boss-1", 1, 0, null, "jinhsi");

    private static ScheduleDraft draft(DayPlan day, List<Coverage> coverage, List<String> warnings) {
        return new ScheduleDraft(List.of("jinhsi"), List.of(day), coverage, false, true, warnings);
    }

    private static DayPlan day(int waveplatesAvailable, int minutesAvailable, DraftTask... tasks) {
        int waveplates = 0;
        int minutes = 0;
        for (DraftTask task : tasks) {
            waveplates += task.waveplates();
            minutes += task.minutes();
        }
        return new DayPlan(DAY, false, waveplatesAvailable, minutesAvailable, waveplates, minutes, List.of(tasks));
    }

    private static final List<Coverage> COVERAGE = List.of(new Coverage("metallic-drip", "Metallic Drip", 12, 8));
    private static final List<String> WARNINGS = List.of("Servono altri 3 giorni.");

    @Test
    void isTheSamePlanWhenOnlyTheWaveplatesAndMinutesAvailableChanged() {
        ScheduleDraft seen = draft(day(120, 60, FORGERY), COVERAGE, WARNINGS);
        // A Waveplate came back and the user played a few minutes less: the tasks are the same
        ScheduleDraft now = draft(day(121, 55, FORGERY), COVERAGE, WARNINGS);

        assertThat(now).isNotEqualTo(seen);
        assertThat(now.samePlanAs(seen)).isTrue();
    }

    @Test
    void isAnotherPlanWhenTheTasksTheCoverageOrTheWarningsChanged() {
        ScheduleDraft seen = draft(day(240, 60, FORGERY), COVERAGE, WARNINGS);

        assertThat(draft(day(240, 60, FORGERY, BOSS), COVERAGE, WARNINGS).samePlanAs(seen)).isFalse();
        assertThat(draft(day(240, 60, FORGERY), List.of(), WARNINGS).samePlanAs(seen)).isFalse();
        assertThat(draft(day(240, 60, FORGERY), COVERAGE, List.of()).samePlanAs(seen)).isFalse();
        assertThat(new ScheduleDraft(List.of("aalto"), List.of(day(240, 60, FORGERY)), COVERAGE, false, true, WARNINGS)
                .samePlanAs(seen)).isFalse();
        assertThat(new ScheduleDraft(List.of("jinhsi"), List.of(day(240, 60, FORGERY)), COVERAGE, true, true, WARNINGS)
                .samePlanAs(seen)).isFalse();
    }

    @Test
    void isAnotherPlanWhenTheDaysAreDifferent() {
        ScheduleDraft seen = draft(day(240, 60, FORGERY), COVERAGE, WARNINGS);
        DayPlan nextDay = new DayPlan(DAY.plusDays(1), false, 240, 60, 80, 30, List.of(FORGERY));
        DayPlan excluded = new DayPlan(DAY, true, 240, 60, 80, 30, List.of(FORGERY));

        assertThat(draft(nextDay, COVERAGE, WARNINGS).samePlanAs(seen)).isFalse();
        assertThat(draft(excluded, COVERAGE, WARNINGS).samePlanAs(seen)).isFalse();
        assertThat(new ScheduleDraft(List.of("jinhsi"), List.of(day(240, 60, FORGERY), nextDay), COVERAGE, false,
                true, WARNINGS).samePlanAs(seen)).isFalse();
        assertThat(seen.samePlanAs(null)).isFalse();
    }
}
