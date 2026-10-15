package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a farming task adds to the inventory when it is marked as done (D21, D94, D118). Pure calculation: every drop of
 * the source gives its average quantity for each run, and the runs inside the daily limit of an event are multiplied.
 * The total of the task is rounded down, so the inventory never counts a unit that the game may not have given.
 */
public final class CompletionRewards {

    // A missing limit means every run of the day gets the multiplier (D94)
    private static final Comparator<Effect> STRENGTH = Comparator.comparing(Effect::multiplier)
            .thenComparing(Effect::dailyLimit, Comparator.nullsLast(Comparator.naturalOrder()));

    private CompletionRewards() {
        // only static methods
    }

    /** The runs that get the multiplier: the first ones of the day, up to the daily limit (null = no limit). */
    public static int boostedRuns(int runs, Integer dailyLimit, long runsAlreadyDone) {
        if (dailyLimit == null) {
            return runs;
        }
        long left = Math.max(0, dailyLimit - runsAlreadyDone);
        return (int) Math.min(runs, left);
    }

    /**
     * The quantity of each material, in the order of the drops: the runs without the multiplier count once, the
     * boosted ones count {@code multiplier} times. A material whose total rounds down to 0 is left out.
     */
    public static Map<String, Long> gained(List<Drop> drops, int runs, int boostedRuns, BigDecimal multiplier) {
        if (runs <= 0 || boostedRuns < 0 || boostedRuns > runs) {
            throw new IllegalArgumentException("Impossible runs: " + runs + " of which boosted " + boostedRuns);
        }
        BigDecimal effectiveRuns = BigDecimal.valueOf(runs - boostedRuns)
                .add(multiplier.multiply(BigDecimal.valueOf(boostedRuns)));
        Map<String, Long> gained = new LinkedHashMap<>();
        // A source lists each material once (unique key of farming_drop), so this is the total of the material
        for (Drop drop : drops) {
            long quantity = drop.avgQuantity().multiply(effectiveRuns).setScale(0, RoundingMode.FLOOR).longValueExact();
            if (quantity > 0) {
                gained.merge(drop.materialId(), quantity, Math::addExact);
            }
        }
        return gained;
    }

    /** The effect of the events on that type of source with the highest multiplier, then the highest daily limit. */
    public static Optional<Effect> strongest(List<Effect> effects, FarmingSourceType type) {
        return effects.stream()
                .filter(effect -> effect.sourceType() == type)
                .max(STRENGTH);
    }
}
