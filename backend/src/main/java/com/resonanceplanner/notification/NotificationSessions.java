package com.resonanceplanner.notification;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

/**
 * The open WebSocket connections (D135), in memory: Render runs one instance of the server. A connection gets its
 * user when the AUTH message brings a valid token; before that it receives nothing. A user with several tabs has
 * several connections, and every notice goes to all of them.
 */
@Component
public class NotificationSessions {

    private static final Logger log = LoggerFactory.getLogger(NotificationSessions.class);

    /** The browser sends PING every 25 seconds: a connection silent for longer than this is gone. */
    static final Duration SILENCE_LIMIT = Duration.ofSeconds(90);
    /** Token refused or expired, or user deleted: the browser does not try again with the same token. */
    static final CloseStatus TOKEN_REJECTED = new CloseStatus(4401, "Token non valido");
    /** Nothing arrived for too long: the browser may open a new connection. */
    static final CloseStatus SILENT = new CloseStatus(4408, "Connessione silenziosa");

    // A notice is a few hundred bytes: a browser that does not read them is dropped, it does not fill the memory
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public NotificationSessions(JsonMapper jsonMapper, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    // userId and expiresAt are null until the token arrives
    private record Connection(WebSocketSession session, Long userId, Instant expiresAt, Instant lastSeen) {

        boolean authenticated() {
            return userId != null;
        }
    }

    public void opened(WebSocketSession session) {
        // Two sends on the same connection at the same moment (the job, the check after AUTH) would fail: the
        // decorator puts them in a queue
        WebSocketSession queued = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        connections.put(session.getId(), new Connection(queued, null, null, clock.instant()));
    }

    /** Any message from the browser, PING included, says the connection is alive. */
    public void seen(String sessionId) {
        Instant now = clock.instant();
        connections.computeIfPresent(sessionId,
                (id, connection) -> new Connection(connection.session(), connection.userId(), connection.expiresAt(), now));
    }

    /** False if the connection closed in the meantime. */
    public boolean authenticate(String sessionId, Long userId, Instant expiresAt) {
        return connections.computeIfPresent(sessionId,
                (id, connection) -> new Connection(connection.session(), userId, expiresAt, connection.lastSeen())) != null;
    }

    public boolean isAuthenticated(String sessionId) {
        Connection connection = connections.get(sessionId);
        return connection != null && connection.authenticated();
    }

    public void closed(String sessionId) {
        connections.remove(sessionId);
    }

    /** The users with at least one connection that sent its token. */
    public Set<Long> userIds() {
        return connections.values().stream()
                .map(Connection::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** True if at least one connection of the user received the notice. */
    public boolean send(Long userId, Notice notice) {
        return deliver(connection -> userId.equals(connection.userId()), notice.type(), notice.message()) > 0;
    }

    public void broadcast(NotificationType type, String message) {
        deliver(Connection::authenticated, type, message);
    }

    /** Closes the connections whose token has expired, and those that stayed silent too long (with or without a user). */
    public void closeSilentOrExpired() {
        Instant now = clock.instant();
        for (Connection connection : connections.values()) {
            if (connection.authenticated() && !now.isBefore(connection.expiresAt())) {
                close(connection, TOKEN_REJECTED);
            } else if (Duration.between(connection.lastSeen(), now).compareTo(SILENCE_LIMIT) > 0) {
                close(connection, SILENT);
            }
        }
    }

    /** Closes the connections of users that no longer exist (D102). */
    public void closeUsers(Collection<Long> userIds) {
        connections.values().stream()
                .filter(connection -> connection.authenticated() && userIds.contains(connection.userId()))
                .forEach(connection -> close(connection, TOKEN_REJECTED));
    }

    // How many connections received the message
    private int deliver(Predicate<Connection> recipients, NotificationType type, String message) {
        TextMessage json = new TextMessage(jsonMapper.writeValueAsString(new NotificationMessage(type, message)));
        int delivered = 0;
        for (Connection connection : connections.values()) {
            if (!recipients.test(connection)) {
                continue;
            }
            try {
                connection.session().sendMessage(json);
                delivered++;
            } catch (IOException | RuntimeException e) {
                // A tab closed without saying goodbye, or a browser too slow to read: only that connection ends.
                // Not an error of the server, and the message is not logged (it has the user's data).
                log.debug("WebSocket send failed: {}", e.getClass().getSimpleName());
                close(connection, CloseStatus.SERVER_ERROR);
            }
        }
        return delivered;
    }

    private void close(Connection connection, CloseStatus status) {
        connections.remove(connection.session().getId());
        try {
            connection.session().close(status);
        } catch (IOException e) {
            log.debug("WebSocket close failed: {}", e.getClass().getSimpleName());
        }
    }
}
