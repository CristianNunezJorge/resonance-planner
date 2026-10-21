package com.resonanceplanner.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resonanceplanner.testsupport.MutableClock;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

// The open connections of D135, each with its user once the token arrived
class NotificationSessionsTest {

    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");
    private static final Instant TOKEN_EXPIRY = NOW.plus(Duration.ofHours(20));
    private static final Notice NOTICE = new Notice(NotificationType.WAVEPLATE_ALMOST_FULL,
            "Waveplate quasi piene: 230/240, piene alle 15:00.", "waveplate", "reading");
    private static final TextMessage NOTICE_JSON = new TextMessage(
            "{\"type\":\"WAVEPLATE_ALMOST_FULL\",\"message\":\"Waveplate quasi piene: 230/240, piene alle 15:00.\"}");

    private final MutableClock clock = new MutableClock(NOW);
    private final NotificationSessions sessions = new NotificationSessions(JsonMapper.builder().build(), clock);

    private static WebSocketSession connection(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private WebSocketSession signedIn(String id, long userId) {
        WebSocketSession session = connection(id);
        sessions.opened(session);
        sessions.authenticate(id, userId, TOKEN_EXPIRY);
        return session;
    }

    @Test
    void sendsANoticeToEveryConnectionOfItsUserOnly() throws IOException {
        WebSocketSession firstTab = signedIn("a", 1L);
        WebSocketSession secondTab = signedIn("b", 1L);
        WebSocketSession otherUser = signedIn("c", 2L);

        assertThat(sessions.send(1L, NOTICE)).isTrue();

        verify(firstTab).sendMessage(NOTICE_JSON);
        verify(secondTab).sendMessage(NOTICE_JSON);
        verify(otherUser, never()).sendMessage(any());
    }

    @Test
    void sendsNothingBeforeTheToken() throws IOException {
        WebSocketSession anonymous = connection("a");
        sessions.opened(anonymous);

        assertThat(sessions.send(1L, NOTICE)).isFalse();
        sessions.broadcast(NotificationType.EVENTS_CHANGED, "Gli eventi sono cambiati.");

        verify(anonymous, never()).sendMessage(any());
        assertThat(sessions.isAuthenticated("a")).isFalse();
        assertThat(sessions.userIds()).isEmpty();
    }

    @Test
    void broadcastsToEveryUserWithAToken() throws IOException {
        WebSocketSession first = signedIn("a", 1L);
        WebSocketSession second = signedIn("b", 2L);

        sessions.broadcast(NotificationType.EVENTS_CHANGED, "Gli eventi sono cambiati.");

        TextMessage json = new TextMessage("{\"type\":\"EVENTS_CHANGED\",\"message\":\"Gli eventi sono cambiati.\"}");
        verify(first).sendMessage(json);
        verify(second).sendMessage(json);
        assertThat(sessions.userIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void forgetsAClosedConnection() throws IOException {
        WebSocketSession session = signedIn("a", 1L);

        sessions.closed("a");
        sessions.send(1L, NOTICE);

        verify(session, never()).sendMessage(any());
        assertThat(sessions.userIds()).isEmpty();
    }

    @Test
    void closesTheConnectionsThatStayedSilentTooLong() throws IOException {
        WebSocketSession silent = signedIn("a", 1L);
        WebSocketSession talking = signedIn("b", 2L);
        WebSocketSession anonymous = connection("c");
        sessions.opened(anonymous);

        clock.advance(Duration.ofSeconds(60));
        sessions.seen("b");
        clock.advance(Duration.ofSeconds(31));
        sessions.closeSilentOrExpired();

        verify(silent).close(NotificationSessions.SILENT);
        verify(anonymous).close(NotificationSessions.SILENT);
        verify(talking, never()).close(any());
        assertThat(sessions.userIds()).containsExactly(2L);
    }

    @Test
    void closesTheConnectionsWhoseTokenExpired() throws IOException {
        WebSocketSession session = connection("a");
        sessions.opened(session);
        sessions.authenticate("a", 1L, NOW.plusSeconds(30));

        clock.advance(Duration.ofSeconds(30));
        sessions.closeSilentOrExpired();

        verify(session).close(NotificationSessions.TOKEN_REJECTED);
        assertThat(sessions.userIds()).isEmpty();
    }

    @Test
    void closesTheConnectionsOfUsersThatNoLongerExist() throws IOException {
        WebSocketSession gone = signedIn("a", 1L);
        WebSocketSession other = signedIn("b", 2L);

        sessions.closeUsers(List.of(1L));

        verify(gone).close(NotificationSessions.TOKEN_REJECTED);
        verify(other, never()).close(any());
        assertThat(sessions.userIds()).containsExactly(2L);
    }

    @Test
    void aFailedSendClosesThatConnectionAndReachesTheOthers() throws IOException {
        WebSocketSession broken = signedIn("a", 1L);
        WebSocketSession working = signedIn("b", 1L);
        doThrow(new IOException("broken pipe")).when(broken).sendMessage(any());

        assertThat(sessions.send(1L, NOTICE)).isTrue();

        verify(working).sendMessage(NOTICE_JSON);
        verify(broken).close(CloseStatus.SERVER_ERROR);
        assertThat(sessions.userIds()).containsExactly(1L);
    }

    @Test
    void saysWhenTheNoticeReachedNoConnection() throws IOException {
        WebSocketSession broken = signedIn("a", 1L);
        doThrow(new IOException("broken pipe")).when(broken).sendMessage(any());

        assertThat(sessions.send(1L, NOTICE)).isFalse();
    }

    @Test
    void doesNotAuthenticateAConnectionThatIsAlreadyGone() {
        assertThat(sessions.authenticate("missing", 1L, TOKEN_EXPIRY)).isFalse();
        assertThat(sessions.userIds()).isEmpty();
    }
}
