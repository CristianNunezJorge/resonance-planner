package com.resonanceplanner.common.error;

import java.time.Duration;
import java.util.Objects;

/**
 * A limit of a service is reached (429), like the daily messages of the agent (D129). The answer has the header
 * Retry-After with the seconds to wait, like the limits of the filter (D101).
 */
public class TooManyRequestsException extends ApiException {

    private final Duration retryAfter;

    public TooManyRequestsException(ErrorCode errorCode, String message, Duration retryAfter) {
        super(errorCode, message);
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    /** The wait in whole seconds, rounded up and at least 1: a client that waits 0 seconds would ask again at once. */
    public long retryAfterSeconds() {
        long millis = Math.max(0, retryAfter.toMillis());
        return Math.max(1, (millis + 999) / 1000);
    }
}
