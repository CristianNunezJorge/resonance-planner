package com.resonanceplanner.waveplate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** The counter at the moment of the request, with the moments when it gets full (docs/05-api.md). */
public record WaveplateResponse(
        @Schema(description = "Le Waveplate di adesso, calcolate dal server", example = "120") int value,
        @Schema(example = "0") int crystals,
        @Schema(example = "240") int max,
        @Schema(example = "480") int crystalsMax,
        @Schema(description = "Quando le Waveplate saranno piene (o da quando lo sono)", example = "2026-10-23T02:00:00Z")
        Instant fullAt,
        @Schema(description = "Quando saranno pieni anche i Crystals", example = "2026-10-27T02:00:00Z")
        Instant crystalsFullAt,
        @Schema(description = "Quando è stata salvata l'ultima lettura", example = "2026-10-22T12:00:00Z")
        Instant recordedAt) {}
