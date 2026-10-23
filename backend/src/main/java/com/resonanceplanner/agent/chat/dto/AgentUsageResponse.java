package com.resonanceplanner.agent.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * The messages of the user today, the daily limit and when it starts again, at midnight in Italy (D41, D129).
 * callsLimitReached is true when the user has less than a turn of calls to the model left for today, also because of
 * turns that failed; appLimitReached is true when the whole app has; guestLimitReached is true for a guest when all
 * the guests have used their share of the calls (D134). They start again at midnight UTC.
 */
public record AgentUsageResponse(
        @Schema(description = "Messaggi scritti oggi", example = "1") int used,
        @Schema(description = "Messaggi al giorno: 5, o 2 per un ospite", example = "5") int limit,
        @Schema(description = "Mezzanotte in Italia", example = "2026-10-22T22:00:00Z") Instant resetsAt,
        @Schema(description = "true se hai finito le tue chiamate al modello di oggi", example = "false")
        boolean callsLimitReached,
        @Schema(description = "true se le ha finite tutta l'app", example = "false") boolean appLimitReached,
        @Schema(description = "true per un ospite se le hanno finite tutti gli ospiti", example = "false")
        boolean guestLimitReached) {}
