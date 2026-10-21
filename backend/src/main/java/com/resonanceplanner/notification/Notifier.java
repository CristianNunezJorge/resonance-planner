package com.resonanceplanner.notification;

import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.EventsChanged;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.UserWaveplate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Decides and sends the real-time notifications (D135). Every minute it looks only at the connected users: with
 * nobody connected it reads nothing. It also checks one user right after the token arrives, and tells everybody
 * when the admin changed the events.
 */
@Component
public class Notifier {

    static final String EVENTS_CHANGED_MESSAGE = "Gli eventi sono cambiati: il calendario è aggiornato.";

    private final NotificationSessions sessions;
    private final NotificationRules rules;
    private final SentNotices sent;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final Today today;
    private final Clock clock;

    public Notifier(NotificationSessions sessions, NotificationRules rules, SentNotices sent,
            UserRepository userRepository, EventService eventService, Today today, Clock clock) {
        this.sessions = sessions;
        this.rules = rules;
        this.sent = sent;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.today = today;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
    public void run() {
        sessions.closeSilentOrExpired();
        notifyUsers(sessions.userIds());
    }

    /** Right after AUTH: whoever comes back to the page does not wait for the next minute. */
    public void checkNow(Long userId) {
        notifyUsers(Set.of(userId));
    }

    // After the commit of the admin's change (D114), so the calendar that reloads already sees it
    @TransactionalEventListener
    public void eventsChanged(EventsChanged change) {
        sessions.broadcast(NotificationType.EVENTS_CHANGED, EVENTS_CHANGED_MESSAGE);
    }

    private void notifyUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        List<UserWaveplate> users = userRepository.findWaveplates(userIds);
        closeDeletedUsers(userIds, users);
        // The events are the same for everybody (D49): read once for all the users
        LocalDate day = today.dateOf(now);
        List<Notice> endingEvents = rules.endingEvents(eventService.between(day, day.plusDays(1)), day);
        for (UserWaveplate user : users) {
            Stream.concat(rules.waveplate(user.reading(), now).stream(), endingEvents.stream())
                    .filter(notice -> sent.firstTime(user.userId(), notice))
                    .forEach(notice -> send(user.userId(), notice));
        }
    }

    // Marked as sent first, so the job and the check after AUTH never send it twice; undone if nobody received it
    private void send(Long userId, Notice notice) {
        if (!sessions.send(userId, notice)) {
            sent.undo(userId, notice);
        }
    }

    // A user deleted while connected (D102): the token is still valid, but the account is gone
    private void closeDeletedUsers(Set<Long> userIds, List<UserWaveplate> users) {
        Set<Long> found = users.stream().map(UserWaveplate::userId).collect(Collectors.toUnmodifiableSet());
        List<Long> deleted = userIds.stream().filter(id -> !found.contains(id)).toList();
        if (!deleted.isEmpty()) {
            sessions.closeUsers(deleted);
            sent.forget(deleted);
        }
    }
}
