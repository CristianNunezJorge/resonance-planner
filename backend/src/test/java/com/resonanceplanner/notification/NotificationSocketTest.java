package com.resonanceplanner.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.event.AdminEventService;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.ScheduleType;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.event.dto.EventRequest.Schedule;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * The WebSocket of D135 with a real Tomcat: a client plays the browser. The users are committed for real (the server
 * reads them in its own threads), with unique names, and deleted at the end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationSocketTest {

    // The allowed origin of application-test.yml
    private static final String FRONTEND = "http://localhost:5173";
    private static final Duration WAIT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    @Autowired
    private NotificationSessions sessions;

    private final List<Long> createdUsers = new ArrayList<>();
    private Long createdEvent;

    // What the server sends, and how it closes
    private static final class Browser extends TextWebSocketHandler {

        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            closed.complete(status);
        }

        // The first message of this type: the others are skipped, because an event of the demo may end today
        String next(NotificationType type) throws InterruptedException {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                String message = messages.poll(100, MILLISECONDS);
                if (message != null && message.contains("\"type\":\"" + type + "\"")) {
                    return message;
                }
            }
            throw new AssertionError("No " + type + " notification arrived");
        }

        int closeCode() throws Exception {
            return closed.get(WAIT.toSeconds(), SECONDS).getCode();
        }
    }

    @AfterEach
    void deleteWhatTheTestCreated() {
        if (createdEvent != null) {
            jdbc.update("delete from game_event where id = ?", createdEvent);
        }
        createdUsers.forEach(id -> jdbc.update("delete from users where id = ?", id));
    }

    @Test
    void sendsTheWaveplateNoticeRightAfterTheToken() throws Exception {
        Long userId = user(Role.USER, new WaveplateSnapshot(235, 0, clock.instant()));
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(auth(jwtService.generateToken(userId, Role.USER)));

        assertThat(browser.next(NotificationType.WAVEPLATE_ALMOST_FULL))
                .startsWith("{\"type\":\"WAVEPLATE_ALMOST_FULL\",\"message\":\"Waveplate quasi piene: 235/240, piene alle ");
        socket.close();
    }

    @Test
    void tellsTheConnectedUsersWhenTheAdminChangesAnEvent() throws Exception {
        Long userId = user(Role.USER, null);
        Long adminId = user(Role.ADMIN, null);
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);
        socket.sendMessage(auth(jwtService.generateToken(userId, Role.USER)));
        socket.sendMessage(new TextMessage("{\"type\":\"PING\"}"));
        // The server reads the messages in its own threads: the broadcast reaches only connections with a token
        waitUntilConnected(userId);

        createdEvent = adminEventService.create(adminId, eventRequest()).id();

        assertThat(browser.next(NotificationType.EVENTS_CHANGED)).isEqualTo(
                "{\"type\":\"EVENTS_CHANGED\",\"message\":\"Gli eventi sono cambiati: il calendario è aggiornato.\"}");
        assertThat(socket.isOpen()).isTrue();
        socket.close();
    }

    @Test
    void closesWithoutRetryingWhenTheTokenIsRejected() throws Exception {
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(auth("not-a-token"));

        assertThat(browser.closeCode()).isEqualTo(4401);
    }

    @Test
    void rejectsTheTokenOfAUserThatNoLongerExists() throws Exception {
        Long userId = user(Role.USER, null);
        String token = jwtService.generateToken(userId, Role.USER);
        jdbc.update("delete from users where id = ?", userId);
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(auth(token));

        assertThat(browser.closeCode()).isEqualTo(4401);
    }

    @Test
    void closesWhenTheFirstMessageIsNotTheToken() throws Exception {
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(new TextMessage("{\"type\":\"PING\"}"));

        assertThat(browser.closeCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void closesWhenTheTokenArrivesASecondTime() throws Exception {
        Long userId = user(Role.USER, null);
        String token = jwtService.generateToken(userId, Role.USER);
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);
        socket.sendMessage(auth(token));
        waitUntilConnected(userId);

        socket.sendMessage(auth(token));

        assertThat(browser.closeCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void closesWhenAMessageIsNotJson() throws Exception {
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(new TextMessage("ciao"));

        assertThat(browser.closeCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void closesWhenAMessageIsTooLong() throws Exception {
        Browser browser = new Browser();
        WebSocketSession socket = open(browser, FRONTEND);

        socket.sendMessage(auth("x".repeat(NotificationSocketHandler.MAX_MESSAGE_BYTES)));

        assertThat(browser.closeCode()).isEqualTo(CloseStatus.TOO_BIG_TO_PROCESS.getCode());
    }

    @Test
    void refusesAnotherSite() {
        assertThatThrownBy(() -> open(new Browser(), "https://evil.example"))
                .isInstanceOf(ExecutionException.class);
    }

    private WebSocketSession open(Browser browser, String origin) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        return new StandardWebSocketClient()
                .execute(browser, headers, URI.create("ws://localhost:" + port + NotificationSocketConfig.PATH))
                .get(WAIT.toSeconds(), SECONDS);
    }

    private void waitUntilConnected(Long userId) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (!sessions.userIds().contains(userId)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The token was never accepted");
            }
            Thread.sleep(20);
        }
    }

    private static TextMessage auth(String token) {
        return new TextMessage("{\"type\":\"AUTH\",\"token\":\"" + token + "\"}");
    }

    private Long user(Role role, WaveplateSnapshot reading) {
        String unique = "ws." + UUID.randomUUID().toString().substring(0, 8);
        User user = User.create(unique, unique + "@example.com", "hash", role);
        if (reading != null) {
            user.recordWaveplate(reading);
        }
        Long id = userRepository.saveAndFlush(user).getId();
        createdUsers.add(id);
        return id;
    }

    private static EventRequest eventRequest() {
        Schedule schedule = new Schedule(ScheduleType.FIXED, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18),
                null, null, null);
        return new EventRequest("Evento della prova WebSocket", EventType.COMBAT_STAGES, "Evento di prova.", schedule,
                Set.of(), List.of(), List.of(), null);
    }
}
