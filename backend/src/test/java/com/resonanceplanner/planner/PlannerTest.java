package com.resonanceplanner.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCalculator;
import com.resonanceplanner.farming.FarmingPlan.SourceLine;
import com.resonanceplanner.farming.FarmingYields;
import com.resonanceplanner.farming.Need;
import com.resonanceplanner.farming.NeedCalculator;
import com.resonanceplanner.farming.TestFarmingCatalog;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import com.resonanceplanner.planner.PlannerInput.EventTodo;
import com.resonanceplanner.planner.PlannerInput.PlannerDay;
import com.resonanceplanner.planner.ScheduleDraft.Coverage;
import com.resonanceplanner.planner.ScheduleDraft.DayPlan;
import com.resonanceplanner.planner.ScheduleDraft.DraftTask;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.roster.Progress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// The planner day by day (D57, D123) on the test catalog: 1000 EXP for every level, the real drops of the JSON files.
// Jinhsi from level 1 to 40 and rank 0 to 2 needs 39,000 EXP, 28,650 Shell Credit, 3 Elegy Tacet Core, 4 LF and 4 MF
// Howler Core and 4 Loong's Pearl.
class PlannerTest {

    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 14);
    private static final int FULL = 240;
    private static final int ONE_HOUR = 60;
    private static final CharacterGoal JINHSI = new CharacterGoal("jinhsi", new Progress(1, 0, 40, 2));
    private static final CharacterGoal YINLIN = new CharacterGoal("yinlin", new Progress(1, 0, 40, 2));
    private static final Effect DOUBLE_SIM = new Effect(EffectType.DROP_MULTIPLIER,
            FarmingSourceType.SIMULATION_TRAINING, new BigDecimal("2"), 3);

    private final Planner planner = new Planner(TestFarmingCatalog.standard());

    private static PlannerDay day(int index, int waveplates, int minutes) {
        return new PlannerDay(MONDAY.plusDays(index), false, waveplates, minutes, List.of(), Map.of(), List.of());
    }

    private static PlannerDay excluded(int index) {
        return new PlannerDay(MONDAY.plusDays(index), true, FULL, ONE_HOUR, List.of(), Map.of(), List.of());
    }

    private static PlannerDay withEffect(int index, Effect effect, long runsAlreadyDone) {
        return new PlannerDay(MONDAY.plusDays(index), false, FULL, ONE_HOUR, List.of(effect),
                Map.of(effect.sourceType(), runsAlreadyDone), List.of());
    }

    private static PlannerDay withActivities(int index, int minutes, List<EventTodo> activities) {
        return new PlannerDay(MONDAY.plusDays(index), false, FULL, minutes, List.of(), Map.of(), activities);
    }

    private static EventTodo activity(String title, int minutes, ActivityFrequency frequency) {
        return new EventTodo(7L, "Festa", MONDAY, title, minutes, frequency);
    }

    private static PlannerInput input(List<CharacterGoal> characters, Map<String, Long> inventory,
            List<PlannerDay> days) {
        return new PlannerInput(characters, inventory, days, ONE_HOUR);
    }

    private static List<DraftTask> farmingOf(DayPlan day) {
        return day.tasks().stream().filter(task -> task.kind() == TaskKind.FARMING).toList();
    }

    @Nested
    class OneCharacter {

        @Test
        void plansTheSameRunsAsTheFarmingCalculationWhenOneDayIsEnough() {
            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(), List.of(day(0, FULL, ONE_HOUR))));

            DayPlan monday = draft.days().getFirst();
            assertThat(monday.tasks())
                    .extracting(DraftTask::kind, DraftTask::farmingSourceId, DraftTask::runs, DraftTask::boostedRuns,
                            DraftTask::waveplates, DraftTask::minutes, DraftTask::characterId)
                    .containsExactly(
                            tuple(TaskKind.FARMING, "boss-mourning-aix", 2, 0, 120, 8, "jinhsi"),
                            tuple(TaskKind.FARMING, "overworld-howlers", 2, 0, 0, 20, "jinhsi"),
                            tuple(TaskKind.FARMING, "collect-loongs-pearl", 1, 0, 0, 10, "jinhsi"),
                            tuple(TaskKind.FARMING, "sim-resonance-potion", 1, 0, 40, 3, "jinhsi"),
                            tuple(TaskKind.FARMING, "sim-shell-credit", 1, 0, 40, 3, "jinhsi"));
            assertThat(monday.tasks().getFirst().title()).isEqualTo("Fonte boss-mourning-aix");
            assertThat(monday.waveplatesUsed()).isEqualTo(200);
            assertThat(monday.minutesUsed()).isEqualTo(44);

            // The same sources and runs as /api/me/farming-plan (D112)
            Need need = new NeedCalculator(TestFarmingCatalog.standard()).needOf("jinhsi", JINHSI.progress());
            assertThat(new FarmingCalculator(TestFarmingCatalog.standard()).plan(need, Map.of(), ONE_HOUR).sources())
                    .extracting(SourceLine::farmingSourceId, SourceLine::runs)
                    .containsExactly(tuple("boss-mourning-aix", 2L), tuple("overworld-howlers", 2L),
                            tuple("collect-loongs-pearl", 1L), tuple("sim-resonance-potion", 1L),
                            tuple("sim-shell-credit", 1L));
            assertThat(draft.complete()).isTrue();
            assertThat(draft.containsEstimates()).isTrue();
            assertThat(draft.warnings()).isEmpty();
        }

        @Test
        void listsWhatIsMissingAndWhatThePlanCovers() {
            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(), List.of(day(0, FULL, ONE_HOUR))));

            assertThat(draft.coverage())
                    .extracting(Coverage::materialId, Coverage::missing, Coverage::covered)
                    .containsExactly(
                            tuple("elegy-tacet-core", 3L, 3L),
                            tuple("lf-howler-core", 4L, 4L),
                            tuple("mf-howler-core", 4L, 4L),
                            tuple("loongs-pearl", 4L, 4L),
                            tuple(FarmingYields.EXP_ID, 39_000L, 39_000L),
                            tuple(TestFarmingCatalog.CREDIT, 28_650L, 28_650L));
            assertThat(draft.coverage()).extracting(Coverage::name)
                    .contains("Elegy Tacet Core", FarmingYields.EXP_NAME);
        }

        @Test
        void movesTheRestToTheNextDayWhenTheWaveplateEndAndUsesTheMinutesOnTheFreeSources() {
            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(),
                    List.of(day(0, 100, ONE_HOUR), day(1, FULL, ONE_HOUR))));

            // 100 Waveplate: one boss run, then the free sources, then one run of Simulation Training
            assertThat(draft.days().get(0).tasks())
                    .extracting(DraftTask::farmingSourceId, DraftTask::runs)
                    .containsExactly(tuple("boss-mourning-aix", 1), tuple("overworld-howlers", 2),
                            tuple("collect-loongs-pearl", 1), tuple("sim-resonance-potion", 1));
            assertThat(draft.days().get(0).waveplatesUsed()).isEqualTo(100);
            assertThat(draft.days().get(1).tasks())
                    .extracting(DraftTask::farmingSourceId, DraftTask::runs)
                    .containsExactly(tuple("boss-mourning-aix", 1), tuple("sim-shell-credit", 1));
            assertThat(draft.complete()).isTrue();
        }

        @Test
        void leavesTheExcludedDaysEmpty() {
            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(),
                    List.of(day(0, 100, ONE_HOUR), excluded(1), day(2, FULL, ONE_HOUR))));

            assertThat(draft.days()).extracting(DayPlan::date, DayPlan::excluded)
                    .containsExactly(tuple(MONDAY, false), tuple(MONDAY.plusDays(1), true),
                            tuple(MONDAY.plusDays(2), false));
            DayPlan tuesday = draft.days().get(1);
            assertThat(tuesday.tasks()).isEmpty();
            assertThat(tuesday.waveplatesAvailable()).isZero();
            assertThat(tuesday.minutesAvailable()).isZero();
            assertThat(draft.days().get(2).tasks())
                    .extracting(DraftTask::farmingSourceId)
                    .containsExactly("boss-mourning-aix", "sim-shell-credit");
        }

        @Test
        void stopsWhenTheMinutesEndAndSaysHowManyDaysAreStillNeeded() {
            ScheduleDraft draft = planner.plan(new PlannerInput(List.of(JINHSI), Map.of(),
                    List.of(day(0, FULL, 15)), 15));

            // 8 minutes of boss, then 3 + 3 of Simulation Training: the free sources take 10 minutes and do not fit
            assertThat(draft.days().getFirst().tasks())
                    .extracting(DraftTask::farmingSourceId)
                    .containsExactly("boss-mourning-aix", "sim-resonance-potion", "sim-shell-credit");
            assertThat(draft.days().getFirst().minutesUsed()).isEqualTo(14);
            assertThat(draft.complete()).isFalse();
            assertThat(draft.coverage())
                    .filteredOn(line -> line.materialId().equals("lf-howler-core"))
                    .extracting(Coverage::missing, Coverage::covered)
                    .containsExactly(tuple(4L, 0L));
            // Still missing: 2 runs of Howlers and 1 of Loong's Pearl, 30 minutes at 15 a day
            assertThat(draft.warnings()).containsExactly(
                    "Il piano non copre tutto il fabbisogno: al ritmo scelto servono circa altri 2 giorni.");
        }

        @Test
        void usesTheInventoryBeforePlanningRuns() {
            Map<String, Long> inventory = Map.of("elegy-tacet-core", 3L, "lf-howler-core", 4L, "mf-howler-core", 4L,
                    "loongs-pearl", 4L, "premium-resonance-potion", 2L, TestFarmingCatalog.CREDIT, 30_000L);

            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), inventory, List.of(day(0, FULL, ONE_HOUR))));

            assertThat(draft.days().getFirst().tasks()).isEmpty();
            assertThat(draft.coverage()).isEmpty();
            assertThat(draft.complete()).isTrue();
            assertThat(draft.warnings()).containsExactly(
                    "Hai già tutto quello che serve per gli obiettivi del roster: non serve farmare.");
        }
    }

    @Nested
    class DoubleDrops {

        // 10,000 EXP for every level: 390,000 EXP, so Simulation Training needs several runs
        private final Planner hungry = new Planner(TestFarmingCatalog.withExpToNext(expTable(10_000)));

        private static Map<Integer, Integer> expTable(int expPerLevel) {
            Map<Integer, Integer> table = new HashMap<>();
            for (int level = 1; level < 40; level++) {
                table.put(level, expPerLevel);
            }
            return table;
        }

        @Test
        void putsTheBoostedRunsFirstUpToTheDailyLimit() {
            ScheduleDraft draft = hungry.plan(input(List.of(JINHSI), Map.of(), List.of(withEffect(0, DOUBLE_SIM, 0))));

            // 3 boosted runs give 3 × 2 × 69,370 EXP: more than the 390,000 needed
            assertThat(farmingOf(draft.days().getFirst()))
                    .extracting(DraftTask::farmingSourceId, DraftTask::runs, DraftTask::boostedRuns)
                    .startsWith(tuple("sim-resonance-potion", 3, 3), tuple("boss-mourning-aix", 2, 0));
            assertThat(draft.coverage())
                    .filteredOn(line -> line.materialId().equals(FarmingYields.EXP_ID))
                    .extracting(Coverage::covered)
                    .containsExactly(390_000L);
        }

        @Test
        void needsMoreRunsWithoutTheEvent() {
            ScheduleDraft draft = hungry.plan(input(List.of(JINHSI), Map.of(), List.of(day(0, FULL, ONE_HOUR))));

            // The boss comes first, then only 3 runs of Simulation Training fit in the Waveplate left:
            // 34,880 + 3 × 69,370 EXP
            assertThat(farmingOf(draft.days().getFirst()))
                    .filteredOn(task -> task.farmingSourceId().equals("sim-resonance-potion"))
                    .extracting(DraftTask::runs, DraftTask::boostedRuns)
                    .containsExactly(tuple(3, 0));
            assertThat(draft.coverage())
                    .filteredOn(line -> line.materialId().equals(FarmingYields.EXP_ID))
                    .extracting(Coverage::covered)
                    .containsExactly(242_990L);
        }

        @Test
        void countsTheRunsAlreadyDoneThatDayInTheLimit() {
            ScheduleDraft draft = hungry.plan(input(List.of(JINHSI), Map.of(), List.of(withEffect(0, DOUBLE_SIM, 2))));

            // One boosted run is left; the other runs of the same source join the same task
            assertThat(farmingOf(draft.days().getFirst()))
                    .filteredOn(task -> task.farmingSourceId().equals("sim-resonance-potion"))
                    .extracting(DraftTask::runs, DraftTask::boostedRuns)
                    .containsExactly(tuple(3, 1));
        }

        @Test
        void boostsEveryRunWithoutADailyLimit() {
            Effect noLimit = new Effect(EffectType.DROP_MULTIPLIER, FarmingSourceType.SIMULATION_TRAINING,
                    new BigDecimal("2"), null);

            ScheduleDraft draft = hungry.plan(input(List.of(JINHSI), Map.of(), List.of(withEffect(0, noLimit, 0))));

            assertThat(farmingOf(draft.days().getFirst()).getFirst())
                    .extracting(DraftTask::farmingSourceId, DraftTask::runs, DraftTask::boostedRuns)
                    .containsExactly("sim-resonance-potion", 3, 3);
        }

        @Test
        void ignoresAnEffectOnSourcesThatAreNotNeeded() {
            Effect bosses = new Effect(EffectType.DROP_MULTIPLIER, FarmingSourceType.BOSS_CHALLENGE,
                    new BigDecimal("2"), 3);

            ScheduleDraft draft = planner.plan(input(List.of(), Map.of(), List.of(withEffect(0, bosses, 0))));

            assertThat(draft.days().getFirst().tasks()).isEmpty();
        }
    }

    @Nested
    class EventActivities {

        @Test
        void comeFirstAndAreDoneOnceForEachEdition() {
            List<EventTodo> todos = List.of(activity("Obiettivi", 15, ActivityFrequency.DAILY),
                    activity("Drill 1", 15, ActivityFrequency.ONCE), activity("Drill 2", 15, ActivityFrequency.ONCE));

            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(),
                    List.of(withActivities(0, ONE_HOUR, todos), withActivities(1, ONE_HOUR, todos))));

            assertThat(draft.days().get(0).tasks())
                    .extracting(DraftTask::kind, DraftTask::title, DraftTask::eventId, DraftTask::minutes)
                    .startsWith(tuple(TaskKind.EVENT, "Festa: Obiettivi", 7L, 15),
                            tuple(TaskKind.EVENT, "Festa: Drill 1", 7L, 15),
                            tuple(TaskKind.EVENT, "Festa: Drill 2", 7L, 15));
            // The daily one again, the drills not
            assertThat(draft.days().get(1).tasks())
                    .filteredOn(task -> task.kind() == TaskKind.EVENT)
                    .extracting(DraftTask::title)
                    .containsExactly("Festa: Obiettivi");
        }

        @Test
        void skipAnActivityThatDoesNotFitAndPlaceItLater() {
            List<EventTodo> todos = List.of(activity("Obiettivi", 15, ActivityFrequency.DAILY),
                    activity("Drill 1", 15, ActivityFrequency.ONCE));

            ScheduleDraft draft = planner.plan(input(List.of(), Map.of(),
                    List.of(withActivities(0, 20, todos), withActivities(1, ONE_HOUR, todos))));

            assertThat(draft.days().get(0).tasks()).extracting(DraftTask::title)
                    .containsExactly("Festa: Obiettivi");
            assertThat(draft.days().get(0).minutesUsed()).isEqualTo(15);
            assertThat(draft.days().get(1).tasks()).extracting(DraftTask::title)
                    .containsExactly("Festa: Obiettivi", "Festa: Drill 1");
        }

        @Test
        void leaveLessTimeToTheFarming() {
            List<EventTodo> todos = List.of(activity("Drill 1", 15, ActivityFrequency.ONCE),
                    activity("Drill 2", 15, ActivityFrequency.ONCE), activity("Drill 3", 15, ActivityFrequency.ONCE),
                    activity("Drill 4", 15, ActivityFrequency.ONCE));

            ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(), List.of(withActivities(0, 60, todos))));

            assertThat(farmingOf(draft.days().getFirst())).isEmpty();
            assertThat(draft.days().getFirst().minutesUsed()).isEqualTo(60);
        }

        @Test
        void cutALongTitleToTheLengthOfATask() {
            String longName = "E".repeat(130);
            EventTodo todo = new EventTodo(7L, longName, MONDAY, "Drill 1", 15, ActivityFrequency.ONCE);

            ScheduleDraft draft = planner.plan(input(List.of(), Map.of(),
                    List.of(withActivities(0, ONE_HOUR, List.of(todo)))));

            assertThat(draft.days().getFirst().tasks().getFirst().title()).hasSize(120);
        }
    }

    @Nested
    class Priority {

        @Test
        void startsFromTheFirstCharacter() {
            ScheduleDraft jinhsiFirst = planner.plan(input(List.of(JINHSI, YINLIN), Map.of(),
                    List.of(day(0, 60, ONE_HOUR))));
            ScheduleDraft yinlinFirst = planner.plan(input(List.of(YINLIN, JINHSI), Map.of(),
                    List.of(day(0, 60, ONE_HOUR))));

            assertThat(jinhsiFirst.days().getFirst().tasks().getFirst())
                    .extracting(DraftTask::farmingSourceId, DraftTask::characterId)
                    .containsExactly("boss-mourning-aix", "jinhsi");
            assertThat(yinlinFirst.days().getFirst().tasks().getFirst())
                    .extracting(DraftTask::farmingSourceId, DraftTask::characterId)
                    .containsExactly("boss-mech-abomination", "yinlin");
        }

        @Test
        void givesWhatIsLeftOfARunToTheNextCharacter() {
            // 2 premium potions: 40,000 EXP, of which Yinlin takes 39,000 and Jinhsi the other 1,000
            ScheduleDraft draft = planner.plan(input(List.of(YINLIN, JINHSI), Map.of("premium-resonance-potion", 2L),
                    List.of(day(0, 400, 120))));

            // The potions of Yinlin's boss go to Jinhsi, whose EXP is then covered without Simulation Training;
            // one run of Shell Credit is enough for both
            assertThat(draft.days().getFirst().tasks())
                    .extracting(DraftTask::farmingSourceId, DraftTask::runs, DraftTask::characterId)
                    .containsExactly(
                            tuple("boss-mech-abomination", 2, "yinlin"),
                            tuple("overworld-whisperins", 2, "yinlin"),
                            tuple("collect-coriolus", 1, "yinlin"),
                            tuple("sim-shell-credit", 1, "yinlin"),
                            tuple("boss-mourning-aix", 2, "jinhsi"),
                            tuple("overworld-howlers", 2, "jinhsi"),
                            tuple("collect-loongs-pearl", 1, "jinhsi"));
            assertThat(draft.complete()).isTrue();
        }
    }

    @Test
    void saysWhenTheRosterIsEmpty() {
        ScheduleDraft draft = planner.plan(input(List.of(), Map.of(), List.of(day(0, FULL, ONE_HOUR))));

        assertThat(draft.days().getFirst().tasks()).isEmpty();
        assertThat(draft.complete()).isTrue();
        assertThat(draft.containsEstimates()).isFalse();
        assertThat(draft.warnings()).containsExactly(
                "Il roster è vuoto: aggiungi un personaggio con un obiettivo per pianificare il farming.");
    }

    @Test
    void saysWhenEveryDayIsExcluded() {
        ScheduleDraft draft = planner.plan(input(List.of(JINHSI), Map.of(), List.of(excluded(0), excluded(1))));

        assertThat(draft.days()).allMatch(day -> day.tasks().isEmpty());
        assertThat(draft.complete()).isFalse();
        assertThat(draft.warnings()).startsWith("Hai escluso tutti i giorni: il piano è vuoto.");
    }

    @Test
    void givesTheSamePlanForTheSameData() {
        PlannerInput input = input(List.of(JINHSI, YINLIN), Map.of(),
                List.of(day(0, 100, ONE_HOUR), withEffect(1, DOUBLE_SIM, 0), day(2, FULL, 30)));

        assertThat(planner.plan(input)).isEqualTo(planner.plan(input));
    }
}
