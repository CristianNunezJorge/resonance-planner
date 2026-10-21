package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.AgentLoop;
import com.resonanceplanner.agent.ChatMessage.Speaker;
import com.resonanceplanner.agent.chat.dto.AgentUsageResponse;
import com.resonanceplanner.agent.client.ModelCallCounter;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily limits of the agent (D129). Each user has 5 messages a day, counted from the saved messages since midnight
 * in Italy (D40, D41): a failed turn saves nothing, so it does not count there (D61). The calls to the model count from
 * midnight UTC like OpenRouter (D126), also the ones of the failed turns: 24 a day for each user, so one account alone
 * never uses the calls of everyone, and 40 a day in the whole app. A turn starts only if a whole turn of calls is left.
 * A guest (D134) has 2 messages a day, and all the guests together 18 of the 40 calls: the rest stays for the
 * registered users.
 */
@Service
@Transactional(readOnly = true)
public class AgentQuota {

    static final String USER_LIMIT = "Hai usato i %d messaggi di oggi con l'assistente: puoi scrivere di nuovo da "
            + "mezzanotte. Intanto puoi usare il piano base.";
    static final String USER_CALLS_LIMIT = "Per oggi hai finito le richieste all'assistente: anche i messaggi non "
            + "riusciti le consumano. Riprova domani o usa il piano base.";
    static final String APP_LIMIT = "Per oggi l'assistente ha finito le richieste gratuite di tutta l'app: riprova "
            + "domani o usa il piano base.";
    static final String GUEST_LIMIT = "Le prove dell'agente per gli ospiti sono finite per oggi: registrati per "
            + "continuare, oppure usa il piano base.";

    private static final Logger log = LoggerFactory.getLogger(AgentQuota.class);

    private final AgentMessageRepository messageRepository;
    private final ModelCallCounter modelCalls;
    private final UserRepository userRepository;
    private final AppProperties.AgentLimits limits;
    private final Today today;
    private final Clock clock;

    public AgentQuota(AgentMessageRepository messageRepository, ModelCallCounter modelCalls,
            UserRepository userRepository, AppProperties properties, Today today, Clock clock) {
        this.messageRepository = messageRepository;
        this.modelCalls = modelCalls;
        this.userRepository = userRepository;
        this.limits = properties.agentLimits();
        this.today = today;
        this.clock = clock;
    }

    /**
     * Checks that the user can send one more message today.
     *
     * @throws TooManyRequestsException with AGENT_DAILY_LIMIT if the user, the guests or the whole app reached a
     *     daily limit
     * @throws UnauthorizedException if the user of the token no longer exists (401, D102)
     */
    public void requireRoom(Long userId) {
        boolean guest = isGuest(userId);
        int dailyMessages = dailyMessagesOf(guest);
        Instant now = clock.instant();
        if (usedToday(userId) >= dailyMessages) {
            throw new TooManyRequestsException(ErrorCode.AGENT_DAILY_LIMIT,
                    USER_LIMIT.formatted(dailyMessages), Duration.between(now, today.startOfNextDay()));
        }
        if (callsLimitReached(userId)) {
            throw new TooManyRequestsException(ErrorCode.AGENT_DAILY_LIMIT, USER_CALLS_LIMIT,
                    Duration.between(now, modelCalls.nextReset()));
        }
        if (guest && guestLimitReached()) {
            log.info("Agent message of guest {} refused: the calls of the guests of today are over", userId);
            throw new TooManyRequestsException(ErrorCode.AGENT_DAILY_LIMIT, GUEST_LIMIT,
                    Duration.between(now, modelCalls.nextReset()));
        }
        if (appLimitReached()) {
            log.warn("Agent message of user {} refused: the calls to the model of today are over", userId);
            throw new TooManyRequestsException(ErrorCode.AGENT_DAILY_LIMIT, APP_LIMIT,
                    Duration.between(now, modelCalls.nextReset()));
        }
    }

    /**
     * The usage of today, for the counter of the chat.
     *
     * @throws UnauthorizedException if the user of the token no longer exists (401, D102)
     */
    public AgentUsageResponse usage(Long userId) {
        boolean guest = isGuest(userId);
        return new AgentUsageResponse(usedToday(userId), dailyMessagesOf(guest), today.startOfNextDay(),
                callsLimitReached(userId), appLimitReached(), guest && guestLimitReached());
    }

    // The role is read from the database, like the existence of the user: a deleted guest gets 401 (D102)
    private boolean isGuest(Long userId) {
        return userRepository.findRoleById(userId).orElseThrow(UnauthorizedException::new) == Role.GUEST;
    }

    private int dailyMessagesOf(boolean guest) {
        return guest ? limits.guestDailyMessages() : limits.dailyMessages();
    }

    private int usedToday(Long userId) {
        return Math.toIntExact(messageRepository.countByUserIdAndRoleAndCreatedAtGreaterThanEqual(
                userId, Speaker.USER, today.startOfDay()));
    }

    // Less than a whole turn of calls left: a turn that starts could stop halfway
    private boolean callsLimitReached(Long userId) {
        return limits.dailyUserModelCalls() - modelCalls.callsToday(userId) < AgentLoop.MAX_ROUNDS;
    }

    private boolean appLimitReached() {
        return limits.dailyModelCalls() - modelCalls.callsToday() < AgentLoop.MAX_ROUNDS;
    }

    private boolean guestLimitReached() {
        return limits.guestDailyModelCalls() - modelCalls.guestCallsToday() < AgentLoop.MAX_ROUNDS;
    }
}
