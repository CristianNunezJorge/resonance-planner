package com.resonanceplanner.waveplate;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.catalog.GameRules.Regeneration;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.user.WaveplateSnapshot;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * The counter of the game (D28), with no database and no clock: 1 Waveplate every 6 minutes up to 240, and only then
 * 1 Waveplate Crystal every 12 minutes up to 480. The numbers come from game-rules.json. The step that is not
 * finished yet does not count, like in the game.
 */
@Component
public class WaveplateRules {

    private final Regeneration waveplate;
    private final Regeneration crystals;

    public WaveplateRules(GameRules gameRules) {
        this.waveplate = gameRules.waveplate();
        this.crystals = gameRules.waveplateCrystals();
    }

    /** What the counter shows at `now`, for a snapshot that was read earlier. A clock behind the reading changes nothing. */
    public WaveplateState stateAt(WaveplateSnapshot snapshot, Instant now) {
        Duration waveplateStep = Duration.ofMinutes(waveplate.regenMinutes());
        Duration crystalStep = Duration.ofMinutes(crystals.regenMinutes());

        long missingWaveplate = Math.max(0, waveplate.max() - snapshot.value());
        Instant fullAt = snapshot.recordedAt().plus(waveplateStep.multipliedBy(missingWaveplate));
        long missingCrystals = Math.max(0, crystals.max() - snapshot.crystals());
        Instant crystalsFullAt = fullAt.plus(crystalStep.multipliedBy(missingCrystals));

        long value = Math.min(waveplate.max(), snapshot.value() + completedSteps(snapshot.recordedAt(), now, waveplateStep));
        // The Crystals start to accumulate when the Waveplate are full, not from the reading
        long accumulated = Math.min(crystals.max(), snapshot.crystals() + completedSteps(fullAt, now, crystalStep));
        return new WaveplateState((int) value, (int) accumulated, fullAt, crystalsFullAt);
    }

    /**
     * Refuses a reading that cannot exist in the game (D105), naming the field that is wrong. The two values are
     * independent: the Crystals stay when the Waveplate are spent, and only grow again once the Waveplate are full.
     */
    public void requireValidReading(int value, int crystalsValue) {
        if (value < 0 || value > waveplate.max()) {
            throw new FieldValidationException("value", "Le Waveplate vanno da 0 a " + waveplate.max() + ".");
        }
        if (crystalsValue < 0 || crystalsValue > crystals.max()) {
            throw new FieldValidationException("crystals", "I Waveplate Crystals vanno da 0 a " + crystals.max() + ".");
        }
    }

    public int waveplateMax() {
        return waveplate.max();
    }

    public int crystalsMax() {
        return crystals.max();
    }

    private static long completedSteps(Instant from, Instant to, Duration step) {
        return to.isAfter(from) ? Duration.between(from, to).dividedBy(step) : 0;
    }
}
