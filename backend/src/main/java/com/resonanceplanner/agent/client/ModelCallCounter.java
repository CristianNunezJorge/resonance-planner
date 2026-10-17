package com.resonanceplanner.agent.client;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Counts the calls to the model in the database, for each user and for the whole app (D129). The day is in UTC,
 * because the free limit of OpenRouter (50 calls a day for the whole account, D126) starts again at midnight UTC, not
 * at midnight in Italy. The agent loop counts each call before it is made, so also the calls of a turn that fails.
 */
@Component
public class ModelCallCounter {

    private final ModelCallDayRepository repository;
    private final Clock clock;

    public ModelCallCounter(ModelCallDayRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** One more call of the user today, committed at once. */
    public void add(Long userId) {
        repository.addCall(day(), userId);
    }

    /** The calls of the whole app today. A turn running at the same moment may add more right after. */
    public int callsToday() {
        return Math.toIntExact(repository.callsOn(day()));
    }

    /** The calls of one user today. */
    public int callsToday(Long userId) {
        return repository.callsOn(day(), userId).orElse(0);
    }

    /** The next midnight UTC, when the calls start again from zero. */
    public Instant nextReset() {
        return day().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private LocalDate day() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
