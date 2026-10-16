package com.resonanceplanner.planner;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EventRules;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.event.dto.EventResponse.Activity;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCatalog;
import com.resonanceplanner.farming.FarmingCatalogLoader;
import com.resonanceplanner.inventory.InventoryItem;
import com.resonanceplanner.inventory.InventoryItemRepository;
import com.resonanceplanner.planner.PlannerInput.CharacterGoal;
import com.resonanceplanner.planner.PlannerInput.EventTodo;
import com.resonanceplanner.planner.PlannerInput.PlannerDay;
import com.resonanceplanner.planning.PlannedTask;
import com.resonanceplanner.planning.PlannedTaskRepository;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.planning.TaskStatus;
import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.OwnedCharacterRepository;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import com.resonanceplanner.waveplate.WaveplateRules;
import com.resonanceplanner.waveplate.WaveplateState;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The planner for the user of the token (D44, D57, D123): this class checks the parameters and reads roster,
 * inventory, Waveplate, events and the tasks already in the calendar; the plan comes from {@link Planner}. It writes
 * nothing. If the user of the token does not exist anymore it answers 401 (D102).
 */
@Service
@Transactional(readOnly = true)
public class PlannerService {

    public static final int DEFAULT_DAYS = 7;
    public static final int MAX_DAYS = 14;
    public static final int MIN_MINUTES = 15;
    public static final int MAX_MINUTES = 240;
    /** The play time of a user who has not chosen it yet. */
    public static final int DEFAULT_MINUTES = 60;

    private static final List<TaskStatus> IN_CALENDAR = List.of(TaskStatus.CONFIRMED, TaskStatus.DONE);

    private final UserRepository userRepository;
    private final OwnedCharacterRepository ownedCharacterRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PlannedTaskRepository taskRepository;
    private final EventService eventService;
    private final FarmingCatalogLoader catalogLoader;
    private final WaveplateRules waveplateRules;
    private final Today today;
    private final Clock clock;

    public PlannerService(UserRepository userRepository, OwnedCharacterRepository ownedCharacterRepository,
            InventoryItemRepository inventoryItemRepository, PlannedTaskRepository taskRepository,
            EventService eventService, FarmingCatalogLoader catalogLoader, WaveplateRules waveplateRules,
            Today today, Clock clock) {
        this.userRepository = userRepository;
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.taskRepository = taskRepository;
        this.eventService = eventService;
        this.catalogLoader = catalogLoader;
        this.waveplateRules = waveplateRules;
        this.today = today;
        this.clock = clock;
    }

    /** A plan that is not saved: a proposal is made from it (D56, D64). Every wrong parameter in one answer. */
    public ScheduleDraft draft(Long userId, ScheduleRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
        List<OwnedCharacter> roster = ownedCharacterRepository.findAllByUserIdOrderByIdAsc(userId);
        LocalDate todayDate = today.date();
        LocalDate start = request.startDate() == null ? todayDate : request.startDate();
        int days = request.days() == null ? DEFAULT_DAYS : request.days();
        requireValid(request, start, todayDate, days, roster);

        LocalDate end = start.plusDays(days - 1L);
        int minutesPerDay = minutesPerDay(request, user);
        List<EventResponse> editions = eventService.between(start, end);
        TasksInCalendar tasks = new TasksInCalendar(taskRepository.findInRange(userId, firstDayToRead(start, editions),
                end, IN_CALENDAR), start);
        FarmingCatalog catalog = catalogLoader.load();
        Set<LocalDate> excluded = request.excludedDates() == null ? Set.of() : new HashSet<>(request.excludedDates());
        boolean withActivities = !Boolean.FALSE.equals(request.includeEventActivities());

        List<PlannerDay> plannerDays = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int waveplates = date.equals(todayDate) ? waveplatesNow(user, catalog) : catalog.waveplatesPerDay();
            plannerDays.add(new PlannerDay(date, excluded.contains(date), waveplates,
                    Math.max(0, minutesPerDay - tasks.minutesTaken(date)), effectsOn(date, editions),
                    tasks.runsDone(date), withActivities ? activitiesOn(date, editions, tasks) : List.of()));
        }
        PlannerInput input = new PlannerInput(inPriority(roster, request.characterPriority()), inventoryOf(userId),
                plannerDays, minutesPerDay);
        return new Planner(catalog).plan(input);
    }

    private static void requireValid(ScheduleRequest request, LocalDate start, LocalDate todayDate, int days,
            List<OwnedCharacter> roster) {
        Map<String, String> problems = new LinkedHashMap<>();
        if (start.isBefore(todayDate)) {
            problems.put("startDate", "Il piano può iniziare da oggi in poi.");
        } else if (start.isAfter(EventRules.LAST_DATE.minusDays(MAX_DAYS))) {
            problems.put("startDate", EventRules.DATE_WINDOW);
        }
        if (days < 1 || days > MAX_DAYS) {
            problems.put("days", "Il piano copre da 1 a " + MAX_DAYS + " giorni.");
        }
        if (request.excludedDates() != null && request.excludedDates().size() > MAX_DAYS) {
            problems.put("excludedDates", "Puoi escludere al massimo " + MAX_DAYS + " giorni.");
        }
        String priorityProblem = priorityProblem(request.characterPriority(), roster);
        if (priorityProblem != null) {
            problems.put("characterPriority", priorityProblem);
        }
        Integer minutes = request.maxMinutesPerDay();
        if (minutes != null && (minutes < MIN_MINUTES || minutes > MAX_MINUTES)) {
            problems.put("maxMinutesPerDay",
                    "Il tempo di gioco va da " + MIN_MINUTES + " a " + MAX_MINUTES + " minuti al giorno.");
        }
        if (!problems.isEmpty()) {
            throw new FieldValidationException(problems);
        }
    }

    private static String priorityProblem(List<String> priority, List<OwnedCharacter> roster) {
        if (priority == null) {
            return null;
        }
        Set<String> owned = new HashSet<>(roster.stream().map(OwnedCharacter::getCharacterId).toList());
        if (!owned.containsAll(priority)) {
            return "Puoi dare la priorità solo ai personaggi del tuo roster.";
        }
        return new HashSet<>(priority).size() == priority.size()
                ? null
                : "Ogni personaggio può comparire una volta sola.";
    }

    // The characters named first, in that order; the rest of the roster after them, in its order
    private static List<CharacterGoal> inPriority(List<OwnedCharacter> roster, List<String> priority) {
        List<String> order = priority == null ? List.of() : priority;
        return roster.stream()
                .sorted(Comparator.comparingInt((OwnedCharacter owned) -> {
                    int index = order.indexOf(owned.getCharacterId());
                    return index < 0 ? order.size() : index;
                }))
                .map(owned -> new CharacterGoal(owned.getCharacterId(), owned.getProgress()))
                .toList();
    }

    private static int minutesPerDay(ScheduleRequest request, User user) {
        if (request.maxMinutesPerDay() != null) {
            return request.maxMinutesPerDay();
        }
        return user.getPlayTimeMinutes() == null ? DEFAULT_MINUTES : user.getPlayTimeMinutes();
    }

    // A ONCE activity done earlier in its edition counts too, so the tasks are read from the first edition start
    private static LocalDate firstDayToRead(LocalDate start, List<EventResponse> editions) {
        return editions.stream()
                .map(EventResponse::startDate)
                .filter(date -> date.isBefore(start))
                .min(Comparator.naturalOrder())
                .orElse(start);
    }

    // D123, point 2: today the Waveplate of the counter plus the Crystals
    private int waveplatesNow(User user, FarmingCatalog catalog) {
        WaveplateSnapshot snapshot = user.getWaveplate();
        if (snapshot == null) {
            return catalog.waveplatesPerDay();
        }
        WaveplateState state = waveplateRules.stateAt(snapshot, clock.instant());
        return state.value() + state.crystals();
    }

    private static List<Effect> effectsOn(LocalDate date, List<EventResponse> editions) {
        return editions.stream()
                .filter(edition -> edition.activeDates().contains(date))
                .map(EventResponse::effect)
                .filter(Objects::nonNull)
                .toList();
    }

    // D123, point 4: from the unlock day of the edition, and not when already done (that day, or in the edition)
    private static List<EventTodo> activitiesOn(LocalDate date, List<EventResponse> editions, TasksInCalendar tasks) {
        List<EventTodo> todos = new ArrayList<>();
        for (EventResponse edition : editions) {
            if (!edition.activeDates().contains(date)) {
                continue;
            }
            for (Activity activity : edition.activities()) {
                Integer unlockDay = activity.unlockDay();
                if (unlockDay != null && date.isBefore(edition.startDate().plusDays(unlockDay - 1L))) {
                    continue;
                }
                String title = Planner.eventTaskTitle(edition.name(), activity.title());
                boolean daily = activity.frequency() == ActivityFrequency.DAILY;
                boolean done = daily
                        ? tasks.eventDone(edition.id(), title, date, date)
                        : tasks.eventDone(edition.id(), title, edition.startDate(), edition.endDate());
                if (!done) {
                    todos.add(new EventTodo(edition.id(), edition.name(), edition.startDate(), activity.title(),
                            activity.minutes(), activity.frequency()));
                }
            }
        }
        return todos;
    }

    private Map<String, Long> inventoryOf(Long userId) {
        Map<String, Long> inventory = new HashMap<>();
        for (InventoryItem item : inventoryItemRepository.findAllByUserIdOrderByMaterialIdAsc(userId)) {
            inventory.put(item.getMaterialId(), (long) item.getQuantity());
        }
        return inventory;
    }

    /**
     * The tasks already in the calendar, as the planner sees them (D123, point 3): the manual ones and the done ones
     * take their minutes; the confirmed tasks of the agent do not, because a new plan replaces them (D22).
     */
    private static final class TasksInCalendar {

        private final Map<LocalDate, Integer> minutesTaken = new HashMap<>();
        private final Map<LocalDate, Map<FarmingSourceType, Long>> runsDone = new HashMap<>();
        private final List<PlannedTask> doneEvents = new ArrayList<>();

        TasksInCalendar(List<PlannedTask> tasks, LocalDate start) {
            for (PlannedTask task : tasks) {
                boolean done = task.getStatus() == TaskStatus.DONE;
                if (done && task.getKind() == TaskKind.EVENT && task.getEventId() != null) {
                    doneEvents.add(task);
                }
                if (task.getTaskDate().isBefore(start)) {
                    continue;
                }
                if (task.isManual() || done) {
                    minutesTaken.merge(task.getTaskDate(), task.getMinutes(), Integer::sum);
                }
                if (done && task.getKind() == TaskKind.FARMING) {
                    runsDone.computeIfAbsent(task.getTaskDate(), date -> new EnumMap<>(FarmingSourceType.class))
                            .merge(task.getFarmingSource().getType(), (long) task.getRuns(), Long::sum);
                }
            }
        }

        int minutesTaken(LocalDate date) {
            return minutesTaken.getOrDefault(date, 0);
        }

        Map<FarmingSourceType, Long> runsDone(LocalDate date) {
            return runsDone.getOrDefault(date, Map.of());
        }

        boolean eventDone(Long eventId, String title, LocalDate from, LocalDate to) {
            return doneEvents.stream().anyMatch(task -> task.getEventId().equals(eventId)
                    && task.getTitle().equals(title)
                    && !task.getTaskDate().isBefore(from) && !task.getTaskDate().isAfter(to));
        }
    }
}
