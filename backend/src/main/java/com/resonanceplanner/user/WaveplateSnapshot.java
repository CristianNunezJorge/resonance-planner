package com.resonanceplanner.user;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The Waveplate and Waveplate Crystals that the user read in the game, and the moment of the reading (D36). The
 * current values are not saved: they are calculated from this snapshot when they are asked for (D28).
 */
@Embeddable
public record WaveplateSnapshot(int value, int crystals, Instant recordedAt) {

    // PostgreSQL keeps microseconds: cut here, so a snapshot built from the clock equals the one read back from the
    // database, and no caller has to remember it
    public WaveplateSnapshot {
        if (recordedAt != null) {
            recordedAt = recordedAt.truncatedTo(ChronoUnit.MICROS);
        }
    }
}
