package com.resonanceplanner.user;

import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * The Waveplate and Waveplate Crystals that the user read in the game, and the moment of the reading (D36). The
 * current values are not saved: they are calculated from this snapshot when they are asked for (D28).
 */
@Embeddable
public record WaveplateSnapshot(int value, int crystals, Instant recordedAt) {}
