package com.resonanceplanner.user;

import java.time.Instant;

/** The last Waveplate reading of a user, read for the notifications (D135); a user may have none yet. */
public record UserWaveplate(Long userId, Integer value, Integer crystals, Instant recordedAt) {

    /** The reading, or null if the user never saved one. */
    public WaveplateSnapshot reading() {
        return recordedAt == null ? null : new WaveplateSnapshot(value, crystals, recordedAt);
    }
}
