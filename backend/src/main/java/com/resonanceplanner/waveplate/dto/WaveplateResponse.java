package com.resonanceplanner.waveplate.dto;

import java.time.Instant;

/** The counter at the moment of the request, with the moments when it gets full (docs/05-api.md). */
public record WaveplateResponse(int value, int crystals, int max, int crystalsMax, Instant fullAt,
        Instant crystalsFullAt, Instant recordedAt) {}
