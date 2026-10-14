package com.resonanceplanner.waveplate;

import java.time.Instant;

/**
 * The counter of the game at a given moment. A "full at" moment can be in the past, if the counter is already full:
 * it is then the moment when it got full.
 */
public record WaveplateState(int value, int crystals, Instant fullAt, Instant crystalsFullAt) {}
