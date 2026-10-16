package com.resonanceplanner.planning;

import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.planning.dto.CalendarResponse;
import com.resonanceplanner.planning.dto.TaskResponse;
import com.resonanceplanner.user.UserRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The calendar of the user of the token (D51): events and tasks of a range in one answer, so the page loads with a
 * single request. The range has the same rules as the events (at most 62 days).
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    // The tasks of a pending proposal are shown apart, discarded and replaced plans not at all (D22)
    private static final Set<TaskStatus> IN_CALENDAR = EnumSet.of(TaskStatus.CONFIRMED, TaskStatus.DONE);

    private final EventService eventService;
    private final PlannedTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProposalService proposalService;

    public CalendarService(EventService eventService, PlannedTaskRepository taskRepository,
            UserRepository userRepository, ProposalService proposalService) {
        this.eventService = eventService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.proposalService = proposalService;
    }

    public CalendarResponse between(Long userId, LocalDate from, LocalDate to) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
        // The events first: they check the range, before the tasks are read
        List<EventResponse> events = eventService.between(from, to);
        List<TaskResponse> tasks = taskRepository.findInRange(userId, from, to, IN_CALENDAR).stream()
                .map(TaskResponse::from)
                .toList();
        return new CalendarResponse(events, tasks, proposalService.pendingOf(userId).orElse(null),
                proposalService.isPlanOutdated(userId));
    }
}
