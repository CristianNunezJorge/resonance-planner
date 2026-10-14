package com.resonanceplanner.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WaveplateSnapshotTest {

    @Test
    void keepsOnlyMicrosecondsLikePostgreSql() {
        // A snapshot built from the clock and the same snapshot read back from the database must be equal
        WaveplateSnapshot snapshot = new WaveplateSnapshot(180, 0, Instant.parse("2026-10-14T12:00:00.123456789Z"));

        assertThat(snapshot.recordedAt()).isEqualTo(Instant.parse("2026-10-14T12:00:00.123456Z"));
    }

    @Test
    void leavesAMomentThatHasNoNanosecondsAsItIs() {
        Instant moment = Instant.parse("2026-10-14T12:00:00Z");

        assertThat(new WaveplateSnapshot(1, 2, moment)).isEqualTo(new WaveplateSnapshot(1, 2, moment));
        assertThat(new WaveplateSnapshot(1, 2, moment).recordedAt()).isEqualTo(moment);
    }
}
