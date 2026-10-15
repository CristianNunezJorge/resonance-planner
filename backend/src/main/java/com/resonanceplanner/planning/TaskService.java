package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.EventRules;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.inventory.InventoryItem;
import com.resonanceplanner.inventory.InventoryItemRepository;
import com.resonanceplanner.inventory.dto.InventoryItemResponse;
import com.resonanceplanner.planning.dto.CompletionResponse;
import com.resonanceplanner.planning.dto.TaskRequest;
import com.resonanceplanner.planning.dto.TaskResponse;
import com.resonanceplanner.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tasks of the user of the token (D24, D44): manual tasks are created, changed and deleted here; every task in the
 * calendar can be marked as done, and a farming task adds its drops to the inventory (D21, D118). The token stays
 * valid until it expires (D45), so every method answers 401 if its user does not exist anymore (D102).
 */
@Service
@Transactional
public class TaskService {

    /** How many manual tasks a user can have (D117): months of tasks, and a limit to the rows of one account. */
    public static final int MAX_MANUAL_TASKS = 500;

    private static final String TASK_NOT_FOUND = "Attività non trovata.";

    private final PlannedTaskRepository taskRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final Clock clock;

    public TaskService(PlannedTaskRepository taskRepository, InventoryItemRepository inventoryItemRepository,
            UserRepository userRepository, EventService eventService, Clock clock) {
        this.taskRepository = taskRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.clock = clock;
    }

    /** A manual task, already in the calendar. The user is locked, so two requests cannot both pass the limit. */
    public TaskResponse create(Long userId, TaskRequest request) {
        lockUser(userId);
        requireDayInWindow(request.taskDate());
        if (taskRepository.countByUserIdAndOrigin(userId, TaskOrigin.MANUAL) >= MAX_MANUAL_TASKS) {
            throw new ConflictException("Hai già " + MAX_MANUAL_TASKS
                    + " attività: cancellane qualcuna prima di crearne altre.");
        }
        PlannedTask task = PlannedTask.manual(userId, request.title().strip(), request.taskDate(), request.minutes(),
                notesOf(request), clock.instant());
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Replaces every field of a manual task; a task of the agent cannot be changed (409). Like every write here it
     * locks the user, so it never overlaps with a completion of the same task.
     */
    public TaskResponse update(Long userId, Long taskId, TaskRequest request) {
        lockUser(userId);
        PlannedTask task = requireTask(userId, taskId);
        requireDayInWindow(request.taskDate());
        requireManual(task);
        task.changeManual(request.title().strip(), request.taskDate(), request.minutes(), notesOf(request));
        return TaskResponse.from(task);
    }

    public void delete(Long userId, Long taskId) {
        lockUser(userId);
        PlannedTask task = requireTask(userId, taskId);
        requireManual(task);
        taskRepository.delete(task);
    }

    /**
     * Marks a task of the calendar as done. The user is locked first: a task completed twice at the same moment is
     * done once, and two farming tasks of the same day share the daily limit of an event.
     */
    public CompletionResponse complete(Long userId, Long taskId) {
        lockUser(userId);
        PlannedTask task = requireTask(userId, taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new ConflictException("Hai già segnato come fatta questa attività.");
        }
        if (task.getStatus() != TaskStatus.CONFIRMED) {
            throw new ConflictException("Puoi segnare come fatta solo un'attività del calendario.");
        }
        List<InventoryItemResponse> added = task.getKind() == TaskKind.FARMING ? addDrops(userId, task) : List.of();
        task.markDone(clock.instant());
        return new CompletionResponse(TaskResponse.from(task), added);
    }

    // The runs of the task with the strongest event effect of its day on the type of its source (D94, D118); the runs
    // already done that day use up the daily limit first. Counted before this task becomes DONE.
    private List<InventoryItemResponse> addDrops(Long userId, PlannedTask task) {
        FarmingSource source = task.getFarmingSource();
        int runs = task.getRuns();
        Optional<Effect> effect = CompletionRewards.strongest(effectsOn(task.getTaskDate()), source.getType());
        int boostedRuns = 0;
        BigDecimal multiplier = BigDecimal.ONE;
        if (effect.isPresent()) {
            long runsAlreadyDone = taskRepository.sumRuns(userId, task.getTaskDate(), TaskStatus.DONE,
                    source.getType());
            boostedRuns = CompletionRewards.boostedRuns(runs, effect.get().dailyLimit(), runsAlreadyDone);
            multiplier = effect.get().multiplier();
        }
        Map<String, Long> gained = CompletionRewards.gained(dropsOf(source), runs, boostedRuns, multiplier);
        List<InventoryItemResponse> added = new ArrayList<>();
        gained.forEach((materialId, quantity) -> {
            int capped = (int) Math.min(quantity, InventoryItem.MAX_QUANTITY);
            inventoryItemRepository.addQuantity(userId, materialId, capped, InventoryItem.MAX_QUANTITY);
            added.add(new InventoryItemResponse(materialId, capped));
        });
        return added;
    }

    // The effects of the events active on that day (D96)
    private List<Effect> effectsOn(LocalDate day) {
        return eventService.between(day, day).stream()
                .map(EventResponse::effect)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<Drop> dropsOf(FarmingSource source) {
        return source.getDrops().stream()
                .map(drop -> new Drop(drop.getMaterial().getId(), drop.getAvgQuantity(), drop.isEstimated()))
                .toList();
    }

    private PlannedTask requireTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
    }

    private static void requireManual(PlannedTask task) {
        if (!task.isManual()) {
            throw new ConflictException("Puoi modificare e cancellare solo le attività che hai creato tu.");
        }
    }

    // The same window as the dates of the events, far from the limits of LocalDate
    private static void requireDayInWindow(LocalDate day) {
        if (!EventRules.isInWindow(day)) {
            throw new FieldValidationException("taskDate", EventRules.DATE_WINDOW);
        }
    }

    // Empty notes are no notes
    private static String notesOf(TaskRequest request) {
        return request.notes() == null || request.notes().isBlank() ? null : request.notes().strip();
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
    }

    private void lockUser(Long userId) {
        if (userRepository.lockById(userId).isEmpty()) {
            throw new UnauthorizedException();
        }
    }
}
