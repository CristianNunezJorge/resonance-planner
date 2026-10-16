package com.resonanceplanner.planner;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCalculator;
import com.resonanceplanner.farming.FarmingCatalog;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.farming.FarmingCatalog.Source;
import com.resonanceplanner.farming.FarmingYields;
import com.resonanceplanner.farming.Need;
import com.resonanceplanner.planner.NeedPool.Target;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import com.resonanceplanner.planner.PlannerInput.EventTodo;
import com.resonanceplanner.planner.PlannerInput.PlannerDay;
import com.resonanceplanner.planner.ScheduleDraft.Coverage;
import com.resonanceplanner.planner.ScheduleDraft.DayPlan;
import com.resonanceplanner.planner.ScheduleDraft.DraftTask;
import com.resonanceplanner.planning.CompletionRewards;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.TaskKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The planner day by day (D57, D123): event activities first, then the runs boosted by an event up to the daily
 * limit, then the other runs in order of priority, as long as there are Waveplate and minutes. Pure calculation and
 * deterministic: the same data give the same plan. The numbers come from {@link FarmingYields} and
 * {@link CompletionRewards}, the same as the farming calculation and the completion of a task.
 */
public class Planner {

    static final String EMPTY_ROSTER =
            "Il roster è vuoto: aggiungi un personaggio con un obiettivo per pianificare il farming.";
    static final String NOTHING_TO_FARM =
            "Hai già tutto quello che serve per gli obiettivi del roster: non serve farmare.";
    static final String ALL_DAYS_EXCLUDED = "Hai escluso tutti i giorni: il piano è vuoto.";

    private final FarmingCatalog catalog;
    private final FarmingYields yields;

    public Planner(FarmingCatalog catalog) {
        this.catalog = catalog;
        this.yields = new FarmingYields(catalog);
    }

    /** The title of the task of an event activity: the event first, cut to the length of a task. */
    public static String eventTaskTitle(String eventName, String activityTitle) {
        String title = eventName + ": " + activityTitle;
        return title.length() > PlannedTask.MAX_TITLE_LENGTH ? title.substring(0, PlannedTask.MAX_TITLE_LENGTH) : title;
    }

    public ScheduleDraft plan(PlannerInput input) {
        NeedPool pool = new NeedPool(input.characters(), input.inventory(), yields);
        Set<String> onceDone = new HashSet<>();
        List<DayPlan> days = new ArrayList<>();
        for (PlannerDay day : input.days()) {
            days.add(day.excluded()
                    ? new DayPlan(day.date(), true, 0, 0, 0, 0, List.of())
                    : new DayWork(day, pool, onceDone).plan());
        }
        List<Coverage> coverage = pool.initialMaterials().stream()
                .map(materialId -> new Coverage(materialId, yields.nameOf(materialId), pool.initialMissing(materialId),
                        pool.initialMissing(materialId) - pool.stillMissing(materialId)))
                .toList();
        boolean complete = coverage.stream().allMatch(line -> line.covered() == line.missing());
        List<String> priority = input.characters().stream().map(CharacterGoal::characterId).toList();
        return new ScheduleDraft(priority, days, coverage, complete, containsEstimates(days),
                warnings(input, days, pool, complete));
    }

    private List<String> warnings(PlannerInput input, List<DayPlan> days, NeedPool pool, boolean complete) {
        List<String> warnings = new ArrayList<>();
        if (input.characters().isEmpty()) {
            warnings.add(EMPTY_ROSTER);
        } else if (pool.initialMaterials().isEmpty()) {
            warnings.add(NOTHING_TO_FARM);
        }
        if (!days.isEmpty() && days.stream().allMatch(DayPlan::excluded)) {
            warnings.add(ALL_DAYS_EXCLUDED);
        }
        if (!complete) {
            warnings.add(daysStillNeeded(pool, input.minutesPerDay()));
        }
        return warnings;
    }

    // What is still missing, estimated like the farming calculation (D112, point 6) at the chosen play time
    private String daysStillNeeded(NeedPool pool, int minutesPerDay) {
        Map<String, Long> materials = new HashMap<>();
        for (String materialId : pool.initialMaterials()) {
            if (!materialId.equals(FarmingYields.EXP_ID)) {
                materials.put(materialId, pool.stillMissing(materialId));
            }
        }
        Need rest = new Need(pool.stillMissing(FarmingYields.EXP_ID), materials);
        long days = new FarmingCalculator(catalog).plan(rest, Map.of(), minutesPerDay).estimatedDays();
        String needed = days == 1 ? "serve circa un altro giorno" : "servono circa altri " + days + " giorni";
        return "Il piano non copre tutto il fabbisogno: al ritmo scelto " + needed + ".";
    }

    private boolean containsEstimates(List<DayPlan> days) {
        return days.stream()
                .flatMap(day -> day.tasks().stream())
                .filter(task -> task.kind() == TaskKind.FARMING)
                .flatMap(task -> yields.source(task.farmingSourceId()).drops().stream())
                .anyMatch(Drop::estimated);
    }

    /** One day of the plan: the Waveplate and minutes left, and the tasks so far, in order. */
    private final class DayWork {

        private final PlannerDay day;
        private final NeedPool pool;
        private final Set<String> onceDone;
        private final List<TaskDraft> tasks = new ArrayList<>();
        private final Map<String, TaskDraft> farmingBySource = new HashMap<>();
        private int waveplates;
        private int minutes;

        DayWork(PlannerDay day, NeedPool pool, Set<String> onceDone) {
            this.day = day;
            this.pool = pool;
            this.onceDone = onceDone;
            this.waveplates = day.waveplates();
            this.minutes = day.minutes();
        }

        DayPlan plan() {
            placeActivities();
            for (FarmingSourceType type : FarmingSourceType.values()) {
                CompletionRewards.strongest(day.effects(), type).ifPresent(this::placeBoostedRuns);
            }
            placeRuns();
            return new DayPlan(day.date(), false, day.waveplates(), day.minutes(), day.waveplates() - waveplates,
                    day.minutes() - minutes, tasks.stream().map(TaskDraft::toDraft).toList());
        }

        // D123, point 4: a ONCE activity once for each edition; one that does not fit is skipped
        private void placeActivities() {
            for (EventTodo todo : day.activities()) {
                String onceKey = todo.eventId() + "|" + todo.editionStart() + "|" + todo.title();
                boolean once = todo.frequency() == ActivityFrequency.ONCE;
                if ((once && onceDone.contains(onceKey)) || todo.minutes() > minutes) {
                    continue;
                }
                minutes -= todo.minutes();
                tasks.add(TaskDraft.event(eventTaskTitle(todo.eventName(), todo.title()), todo.minutes(),
                        todo.eventId()));
                if (once) {
                    onceDone.add(onceKey);
                }
            }
        }

        // D123, point 5: the runs already done that day use up the limit first, like at the completion (D118)
        private void placeBoostedRuns(Effect effect) {
            long counted = day.runsDone().getOrDefault(effect.sourceType(), 0L);
            while (true) {
                Optional<Pick> pick = firstPick(source -> source.type() == effect.sourceType());
                if (pick.isEmpty()) {
                    return;
                }
                int allowed = CompletionRewards.boostedRuns(pick.get().affordable(), effect.dailyLimit(), counted);
                long perRun = boosted(yieldOf(pick.get()), effect.multiplier());
                if (allowed == 0 || perRun == 0) {
                    return;
                }
                int runs = (int) Math.min(allowed, Math.ceilDiv(pick.get().target().missingCents(), perRun));
                doRuns(pick.get(), runs, runs, effect.multiplier());
                counted += runs;
            }
        }

        // D123, point 6: the first missing material whose source is still possible
        private void placeRuns() {
            while (true) {
                Optional<Pick> pick = firstPick(source -> true);
                if (pick.isEmpty()) {
                    return;
                }
                int runs = (int) Math.min(pick.get().affordable(),
                        Math.ceilDiv(pick.get().target().missingCents(), yieldOf(pick.get())));
                doRuns(pick.get(), runs, 0, BigDecimal.ONE);
            }
        }

        private Optional<Pick> firstPick(Predicate<Source> allowedSource) {
            for (Target target : pool.targets()) {
                Source source = yields.bestSourceOf(target.materialId());
                int affordable = affordable(source);
                if (allowedSource.test(source) && affordable > 0) {
                    return Optional.of(new Pick(target, source, affordable));
                }
            }
            return Optional.empty();
        }

        private int affordable(Source source) {
            long byMinutes = minutes / source.minutesPerRun();
            long byWaveplates = source.waveplateCost() == 0 ? byMinutes : waveplates / source.waveplateCost();
            return (int) Math.min(byMinutes, byWaveplates);
        }

        private long yieldOf(Pick pick) {
            return yields.centsPerRun(pick.source().id()).get(pick.target().materialId());
        }

        // The runs inside the limit give the multiplier times their average, rounded down like at the completion
        private void doRuns(Pick pick, int runs, int boostedRuns, BigDecimal multiplier) {
            Source source = pick.source();
            Map<String, Long> gained = new HashMap<>();
            yields.centsPerRun(source.id()).forEach((materialId, cents) -> gained.put(materialId, Math.addExact(
                    Math.multiplyExact(cents, (long) runs - boostedRuns),
                    Math.multiplyExact(boosted(cents, multiplier), boostedRuns))));
            pool.add(gained);
            waveplates -= runs * source.waveplateCost();
            minutes -= runs * source.minutesPerRun();
            TaskDraft task = farmingBySource.computeIfAbsent(source.id(), id -> {
                TaskDraft created = TaskDraft.farming(source, pick.target().characterId());
                tasks.add(created);
                return created;
            });
            task.addRuns(runs, boostedRuns);
        }

        private static long boosted(long cents, BigDecimal multiplier) {
            return BigDecimal.valueOf(cents).multiply(multiplier).setScale(0, RoundingMode.FLOOR).longValueExact();
        }
    }

    private record Pick(Target target, Source source, int affordable) {}

    /** A task while its day is planned: the runs of the same source on the same day join one task (D123, point 7). */
    private static final class TaskDraft {

        private final TaskKind kind;
        private final String title;
        private final Source source;
        private final Long eventId;
        private final String characterId;
        private final int eventMinutes;
        private int runs;
        private int boostedRuns;

        private TaskDraft(TaskKind kind, String title, Source source, Long eventId, String characterId,
                int eventMinutes) {
            this.kind = kind;
            this.title = title;
            this.source = source;
            this.eventId = eventId;
            this.characterId = characterId;
            this.eventMinutes = eventMinutes;
        }

        static TaskDraft event(String title, int minutes, Long eventId) {
            return new TaskDraft(TaskKind.EVENT, title, null, eventId, null, minutes);
        }

        static TaskDraft farming(Source source, String characterId) {
            return new TaskDraft(TaskKind.FARMING, source.name(), source, null, characterId, 0);
        }

        void addRuns(int more, int moreBoosted) {
            runs += more;
            boostedRuns += moreBoosted;
        }

        DraftTask toDraft() {
            if (kind == TaskKind.EVENT) {
                return new DraftTask(kind, title, eventMinutes, 0, null, null, null, eventId, null);
            }
            return new DraftTask(kind, title, runs * source.minutesPerRun(), runs * source.waveplateCost(),
                    source.id(), runs, boostedRuns, null, characterId);
        }
    }
}
