package com.resonanceplanner.agent.chat;

import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The turns of the agent that are running (D129): one at a time for each user, so the count of the messages of the
 * day and the saving never overlap, and at most two in the whole app, so the calls stay under the 20 a minute of
 * OpenRouter (D126). In memory: a turn that is running ends with the server anyway (on Render there is one).
 */
@Component
public class AgentTurnGate {

    static final String USER_BUSY = "Aspetta la risposta al messaggio di prima.";
    static final String APP_BUSY = "L'assistente sta già rispondendo ad altri utenti: riprova tra un minuto.";
    static final Duration RETRY_AFTER = Duration.ofMinutes(1);

    private final Set<Long> running = new HashSet<>();
    private final int maxTurns;

    public AgentTurnGate(AppProperties properties) {
        this.maxTurns = properties.agentLimits().concurrentTurns();
    }

    /**
     * Starts a turn of the user; closing the pass ends it.
     *
     * @throws TooManyRequestsException if the user has a turn running, or the whole app has as many as it allows
     */
    public synchronized Pass enter(Long userId) {
        if (running.contains(userId)) {
            throw new TooManyRequestsException(ErrorCode.RATE_LIMITED, USER_BUSY, RETRY_AFTER);
        }
        if (running.size() >= maxTurns) {
            throw new TooManyRequestsException(ErrorCode.RATE_LIMITED, APP_BUSY, RETRY_AFTER);
        }
        running.add(userId);
        return new Pass(userId);
    }

    /** How many turns are running; the tests use it. */
    synchronized int running() {
        return running.size();
    }

    private synchronized void leave(Long userId) {
        running.remove(userId);
    }

    /** One running turn. Closing it twice ends it once, so it never ends a newer turn of the same user. */
    public final class Pass implements AutoCloseable {

        private final Long userId;
        private boolean closed;

        private Pass(Long userId) {
            this.userId = userId;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                leave(userId);
            }
        }
    }
}
