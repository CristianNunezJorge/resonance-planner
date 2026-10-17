package com.resonanceplanner.agent.chat.dto;

import java.time.Instant;

/**
 * The messages of the user today, the daily limit and when it starts again, at midnight in Italy (D41, D129).
 * callsLimitReached is true when the user has less than a turn of calls to the model left for today, also because of
 * turns that failed; appLimitReached is true when the whole app has. Both start again at midnight UTC.
 */
public record AgentUsageResponse(int used, int limit, Instant resetsAt, boolean callsLimitReached,
        boolean appLimitReached) {}
