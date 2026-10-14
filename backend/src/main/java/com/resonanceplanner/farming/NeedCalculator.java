package com.resonanceplanner.farming;

import com.resonanceplanner.farming.FarmingCatalog.Ascension;
import com.resonanceplanner.roster.Progress;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a character needs to go from its present to its goal (D112, point 1): the EXP of every level in between, the
 * Shell Credit for all that EXP (D93, rounded up) and the cost of every ascension after the present rank, up to the
 * goal rank included. Pure calculation, no database. The progress is already valid (D104).
 */
public class NeedCalculator {

    private final FarmingCatalog catalog;

    public NeedCalculator(FarmingCatalog catalog) {
        this.catalog = catalog;
    }

    public Need needOf(String characterId, Progress progress) {
        long exp = expBetween(progress.currentLevel(), progress.targetLevel());
        Map<String, Long> materials = new HashMap<>();
        long levelCredits = creditsFor(exp);
        if (levelCredits > 0) {
            materials.put(catalog.creditMaterialId(), levelCredits);
        }
        for (int rank = progress.currentRank() + 1; rank <= progress.targetRank(); rank++) {
            ascension(characterId, rank).cost()
                    .forEach((materialId, quantity) -> materials.merge(materialId, (long) quantity, Math::addExact));
        }
        return new Need(exp, materials);
    }

    private long expBetween(int fromLevel, int toLevel) {
        long exp = 0;
        for (int level = fromLevel; level < toLevel; level++) {
            Integer expToNext = catalog.expToNext().get(level);
            if (expToNext == null) {
                throw new IllegalStateException("The EXP table has no value for level " + level);
            }
            exp = Math.addExact(exp, expToNext);
        }
        return exp;
    }

    // Credits are whole: a fraction is rounded up, because it still has to be paid
    private long creditsFor(long exp) {
        return BigDecimal.valueOf(exp)
                .multiply(catalog.shellCreditPerExp())
                .setScale(0, RoundingMode.CEILING)
                .longValueExact();
    }

    // A missing ascension is an error of the data: counting it as free would hide it
    private Ascension ascension(String characterId, int rank) {
        return catalog.ascensions().getOrDefault(characterId, List.of()).stream()
                .filter(ascension -> ascension.toRank() == rank)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "The catalog has no ascension to rank " + rank + " for " + characterId));
    }
}
