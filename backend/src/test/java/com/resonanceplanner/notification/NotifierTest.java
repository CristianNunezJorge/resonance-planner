package com.resonanceplanner.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.EventsChanged;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.UserWaveplate;
import com.resonanceplanner.waveplate.WaveplateRules;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

// The job of every minute and the check right after AUTH (D135), with fake connections and data
class NotifierTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-21");
    private static final EventResponse ENDING_TODAY = new EventResponse(7L, "Bountiful Crescendo", EventType.DOUBLE_DROP,
            "", TODAY.minusDays(7), TODAY, null, List.of(), List.of(), List.of(), null);

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final NotificationSessions sessions = mock(NotificationSessions.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EventService eventService = mock(EventService.class);
    private final Notifier notifier = new Notifier(sessions,
            new NotificationRules(new WaveplateRules(TestGameRules.standard()), ROME), new SentNotices(),
            userRepository, eventService, new Today(clock, ROME), clock);

    private static Notice ofType(NotificationType type) {
        return argThat(notice -> notice != null && notice.type() == type);
    }

    @Test
    void sendsTheNoticesOfEveryConnectedUserOnlyOnce() {
        when(sessions.send(any(), any())).thenReturn(true);
        when(sessions.userIds()).thenReturn(Set.of(1L, 2L));
        when(userRepository.findWaveplates(Set.of(1L, 2L))).thenReturn(List.of(
                new UserWaveplate(1L, 235, 0, NOW), new UserWaveplate(2L, null, null, null)));
        when(eventService.between(TODAY, TODAY.plusDays(1))).thenReturn(List.of(ENDING_TODAY));

        notifier.run();
        notifier.run();

        verify(sessions, times(2)).closeSilentOrExpired();
        verify(sessions).send(eq(1L), ofType(NotificationType.WAVEPLATE_ALMOST_FULL));
        verify(sessions).send(eq(1L), ofType(NotificationType.EVENT_ENDING));
        // Without a reading there is no Waveplate notice, but the events are the same for everybody
        verify(sessions).send(eq(2L), ofType(NotificationType.EVENT_ENDING));
        verify(sessions, times(3)).send(any(), any());
    }

    @Test
    void keepsANoticeThatReachedNoConnectionForTheNextRound() {
        // The tab closed between the list of the connected users and the send: the notice must not be lost
        when(sessions.send(any(), any())).thenReturn(false, true);
        when(sessions.userIds()).thenReturn(Set.of(1L));
        when(userRepository.findWaveplates(Set.of(1L))).thenReturn(List.of(new UserWaveplate(1L, 235, 0, NOW)));
        when(eventService.between(TODAY, TODAY.plusDays(1))).thenReturn(List.of());

        notifier.run();
        notifier.run();
        notifier.run();

        verify(sessions, times(2)).send(eq(1L), ofType(NotificationType.WAVEPLATE_ALMOST_FULL));
    }

    @Test
    void readsNothingWhenNobodyIsConnected() {
        when(sessions.userIds()).thenReturn(Set.of());

        notifier.run();

        verify(sessions).closeSilentOrExpired();
        verifyNoInteractions(userRepository, eventService);
    }

    @Test
    void closesTheConnectionsOfUsersThatNoLongerExist() {
        when(sessions.userIds()).thenReturn(Set.of(1L, 2L));
        when(userRepository.findWaveplates(Set.of(1L, 2L))).thenReturn(List.of(new UserWaveplate(2L, 100, 0, NOW)));
        when(eventService.between(TODAY, TODAY.plusDays(1))).thenReturn(List.of(ENDING_TODAY));

        notifier.run();

        verify(sessions).closeUsers(List.of(1L));
        verify(sessions, never()).send(eq(1L), any());
    }

    @Test
    void checksOneUserRightAfterTheToken() {
        when(userRepository.findWaveplates(Set.of(1L))).thenReturn(List.of(new UserWaveplate(1L, 240, 0, NOW.minusSeconds(60))));
        when(eventService.between(TODAY, TODAY.plusDays(1))).thenReturn(List.of(ENDING_TODAY));

        notifier.checkNow(1L);

        verify(sessions).send(eq(1L), ofType(NotificationType.EVENT_ENDING));
        verify(sessions, never()).closeSilentOrExpired();
    }

    @Test
    void tellsEveryConnectedUserThatTheEventsChanged() {
        notifier.eventsChanged(new EventsChanged());

        verify(sessions).broadcast(NotificationType.EVENTS_CHANGED, "Gli eventi sono cambiati: il calendario è aggiornato.");
    }
}
