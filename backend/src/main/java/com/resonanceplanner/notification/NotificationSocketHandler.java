package com.resonanceplanner.notification;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.auth.VerifiedToken;
import com.resonanceplanner.user.UserRepository;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Our small protocol on /ws (D135). The browser cannot put the token in a header of the WebSocket, and in the address
 * it would end up in the logs of the proxies: so the first message is {"type":"AUTH","token":"…"}, and then only
 * {"type":"PING"} every 25 seconds. Anything else closes the connection. The server only sends notifications.
 */
@Component
public class NotificationSocketHandler extends TextWebSocketHandler {

    /** A token has at most 2048 characters (D103): every message of the browser fits in 8 KB. */
    static final int MAX_MESSAGE_BYTES = 8 * 1024;
    private static final String AUTH = "AUTH";
    private static final String PING = "PING";

    private final NotificationSessions sessions;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final Notifier notifier;
    private final JsonMapper jsonMapper;

    public NotificationSocketHandler(NotificationSessions sessions, JwtService jwtService,
            UserRepository userRepository, Notifier notifier, JsonMapper jsonMapper) {
        this.sessions = sessions;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.notifier = notifier;
        this.jsonMapper = jsonMapper;
    }

    // What the browser sends; the token is there only in AUTH
    record ClientMessage(String type, String token) {}

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(MAX_MESSAGE_BYTES);
        sessions.opened(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        sessions.seen(session.getId());
        Optional<ClientMessage> received = read(message.getPayload());
        if (sessions.isAuthenticated(session.getId())) {
            if (!received.map(ClientMessage::type).filter(PING::equals).isPresent()) {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            return;
        }
        if (!received.map(ClientMessage::type).filter(AUTH::equals).isPresent()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        // The token of a deleted user is still valid for up to 24 hours (D102): the account must exist
        Optional<VerifiedToken> token = jwtService.verify(received.get().token())
                .filter(verified -> userRepository.existsById(verified.user().id()));
        if (token.isEmpty()) {
            session.close(NotificationSessions.TOKEN_REJECTED);
            return;
        }
        Long userId = token.get().user().id();
        if (sessions.authenticate(session.getId(), userId, token.get().expiresAt())) {
            notifier.checkNow(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.closed(session.getId());
    }

    // Not JSON, or not an object: the connection is closed, without logging what arrived (it can hold a token)
    private Optional<ClientMessage> read(String payload) {
        try {
            return Optional.ofNullable(jsonMapper.readValue(payload, ClientMessage.class));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }
}
