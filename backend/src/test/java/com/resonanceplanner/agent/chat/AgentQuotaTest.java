package com.resonanceplanner.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.resonanceplanner.agent.ChatMessage.Speaker;
import com.resonanceplanner.agent.client.ModelCallCounter;
import com.resonanceplanner.agent.chat.dto.AgentUsageResponse;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.testsupport.MutableClock;
import com.resonanceplanner.testsupport.TestAppProperties;
import com.resonanceplanner.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// The daily limits of the agent (D129): 5 messages a day for each user from midnight in Italy; 24 calls to the model a
// day for each user, also of failed turns, and 40 in the whole app, from midnight UTC like OpenRouter. A turn starts
// only if a whole turn of calls is left.
class AgentQuotaTest {

    private static final Long USER = 7L;
    // 14:00 in Rome: the Italian day started at 22:00 UTC of the day before and ends at 22:00 UTC
    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final Instant ITALIAN_MIDNIGHT = Instant.parse("2026-10-16T22:00:00Z");
    private static final Instant NEXT_ITALIAN_MIDNIGHT = Instant.parse("2026-10-17T22:00:00Z");
    private static final Instant NEXT_UTC_MIDNIGHT = Instant.parse("2026-10-18T00:00:00Z");

    private final AgentMessageRepository messages = mock(AgentMessageRepository.class);
    private final ModelCallCounter counter = mock(ModelCallCounter.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MutableClock clock = new MutableClock(NOW);
    private final AgentQuota quota = new AgentQuota(messages, counter, users,
            TestAppProperties.withAgentLimits(new AppProperties.AgentLimits(5, 24, 40, 2, 20)),
            new Today(clock, ZoneId.of("Europe/Rome")), clock);

    @BeforeEach
    void anExistingUserAndAQuietDay() {
        when(users.existsById(USER)).thenReturn(true);
        when(counter.nextReset()).thenReturn(NEXT_UTC_MIDNIGHT);
        usedToday(0);
        callsToday(0);
        userCallsToday(0);
    }

    @Test
    void countsTheMessagesOfTheUserSinceMidnightInItaly() {
        usedToday(3);
        callsToday(10);

        AgentUsageResponse usage = quota.usage(USER);

        assertThat(usage).isEqualTo(new AgentUsageResponse(3, 5, NEXT_ITALIAN_MIDNIGHT, false, false));
    }

    @Test
    void letsTheFifthMessageOfTheDayStartWhenAWholeTurnOfCallsIsLeft() {
        usedToday(4);
        userCallsToday(18);
        callsToday(34);

        assertThatCode(() -> quota.requireRoom(USER)).doesNotThrowAnyException();
    }

    @Test
    void refusesTheSixthMessageOfTheDayUntilMidnightInItaly() {
        usedToday(5);

        assertThatThrownBy(() -> quota.requireRoom(USER))
                .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AGENT_DAILY_LIMIT);
                    assertThat(error.getMessage()).isEqualTo(AgentQuota.USER_LIMIT.formatted(5));
                    assertThat(error.retryAfter()).isEqualTo(Duration.ofHours(10));
                });
    }

    @Test
    void refusesAUserWhoseFailedTurnsUsedTheCallsOfTheDayUntilMidnightUtc() {
        // No message saved, but 19 of the 24 calls of the user are gone (D129)
        userCallsToday(19);
        callsToday(19);

        assertThatThrownBy(() -> quota.requireRoom(USER))
                .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AGENT_DAILY_LIMIT);
                    assertThat(error.getMessage()).isEqualTo(AgentQuota.USER_CALLS_LIMIT);
                    assertThat(error.retryAfter()).isEqualTo(Duration.ofHours(12));
                });
        assertThat(quota.usage(USER)).isEqualTo(new AgentUsageResponse(0, 5, NEXT_ITALIAN_MIDNIGHT, true, false));
    }

    @Test
    void refusesEveryUserWhenTheAppHasLessThanAWholeTurnOfCallsLeft() {
        callsToday(35);

        assertThatThrownBy(() -> quota.requireRoom(USER))
                .isInstanceOfSatisfying(TooManyRequestsException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.AGENT_DAILY_LIMIT);
                    assertThat(error.getMessage()).isEqualTo(AgentQuota.APP_LIMIT);
                    assertThat(error.retryAfter()).isEqualTo(Duration.ofHours(12));
                });
        assertThat(quota.usage(USER).appLimitReached()).isTrue();
    }

    @Test
    void saysTheAppLimitIsReachedAlsoWhenTheCallsWentBeyondIt() {
        callsToday(46);

        assertThat(quota.usage(USER).appLimitReached()).isTrue();
    }

    @Test
    void answersUnauthorizedWhenTheUserOfTheTokenIsGone() {
        when(users.existsById(USER)).thenReturn(false);

        assertThatThrownBy(() -> quota.usage(USER)).isInstanceOf(UnauthorizedException.class);
    }

    private void usedToday(long count) {
        when(messages.countByUserIdAndRoleAndCreatedAtGreaterThanEqual(USER, Speaker.USER, ITALIAN_MIDNIGHT))
                .thenReturn(count);
    }

    private void callsToday(int calls) {
        when(counter.callsToday()).thenReturn(calls);
    }

    private void userCallsToday(int calls) {
        when(counter.callsToday(USER)).thenReturn(calls);
    }
}
