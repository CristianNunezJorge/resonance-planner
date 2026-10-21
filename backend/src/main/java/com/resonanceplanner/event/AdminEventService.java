package com.resonanceplanner.event;

import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.event.dto.AdminEventResponse;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.event.dto.EventRequest.Effect;
import com.resonanceplanner.event.dto.EventRequest.Schedule;
import com.resonanceplanner.user.UserService;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Events created, changed and deleted by the admin (D14, D114). Every method first checks that the user of the
 * token still exists and is still ADMIN (D102). Every change is announced with {@link EventsChanged} (D135).
 */
@Service
@Transactional
public class AdminEventService {

    private static final String EVENT_NOT_FOUND = "Evento non trovato.";

    private final GameEventRepository gameEventRepository;
    private final EventRules rules;
    private final UserService userService;
    private final ApplicationEventPublisher publisher;

    public AdminEventService(GameEventRepository gameEventRepository, EventRules rules, UserService userService,
            ApplicationEventPublisher publisher) {
        this.gameEventRepository = gameEventRepository;
        this.rules = rules;
        this.userService = userService;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public List<AdminEventResponse> list(Long adminId) {
        userService.requireAdmin(adminId);
        return gameEventRepository.findAllByOrderByIdAsc().stream().map(AdminEventResponse::from).toList();
    }

    public AdminEventResponse create(Long adminId, EventRequest request) {
        userService.requireAdmin(adminId);
        rules.requireValid(request);
        GameEvent event = GameEvent.custom(request.name(), request.type(), request.description());
        apply(event, request);
        AdminEventResponse created = AdminEventResponse.from(gameEventRepository.saveAndFlush(event));
        publisher.publishEvent(new EventsChanged());
        return created;
    }

    /** Replaces everything; a demo event keeps its code (D39). 404 before 400: a missing event has no rules. */
    public AdminEventResponse update(Long adminId, Long id, EventRequest request) {
        userService.requireAdmin(adminId);
        GameEvent event = gameEventRepository.findById(id).orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        rules.requireValid(request);
        apply(event, request);
        gameEventRepository.flush();
        publisher.publishEvent(new EventsChanged());
        return AdminEventResponse.from(event);
    }

    /** One statement: the database deletes days, rewards and activities, and unlinks the planned tasks (D114). */
    public void delete(Long adminId, Long id) {
        userService.requireAdmin(adminId);
        if (gameEventRepository.deleteOne(id) == 0) {
            throw new NotFoundException(EVENT_NOT_FOUND);
        }
        publisher.publishEvent(new EventsChanged());
    }

    private static void apply(GameEvent event, EventRequest request) {
        event.changeDetails(request.name(), request.type(), request.description());
        Schedule schedule = request.schedule();
        if (schedule.type() == ScheduleType.FIXED) {
            event.scheduleFixed(schedule.startDate(), schedule.endDate());
        } else {
            event.scheduleRecurring(schedule.startOffsetDays(), schedule.durationDays(), schedule.repeatEveryDays());
        }
        event.setActiveWeekdays(request.activeWeekdaysOrEvery());
        event.setRewards(request.rewardsOrNone());
        event.removeActivities();
        request.activitiesOrNone().forEach(activity ->
                event.addActivity(activity.title(), activity.minutes(), activity.frequency(), activity.unlockDay()));
        Effect effect = request.effect();
        event.setEffect(effect == null
                ? null
                : new EventEffect(effect.type(), effect.sourceType(), effect.multiplier(), effect.dailyLimit()));
    }
}
