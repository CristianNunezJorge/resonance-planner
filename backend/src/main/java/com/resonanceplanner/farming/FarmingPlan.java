package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.MaterialCategory;
import java.util.List;

/**
 * The result of the farming calculation (D112): every needed material with what is owned and what is missing, the
 * runs of every source in the order they were chosen, the totals and whether some drop is an estimate (D25).
 *
 * @param estimatedDays null when the user has not chosen a daily play time yet
 */
public record FarmingPlan(
        List<MaterialLine> materials,
        List<SourceLine> sources,
        long waveplates,
        long minutes,
        Long estimatedDays,
        boolean containsEstimates) {

    private static final int FULL_PERCENT = 100;

    public FarmingPlan {
        materials = List.copyOf(materials);
        sources = List.copyOf(sources);
    }

    /**
     * How much of the need is already owned, from 0 to 100 (D112, point 7): the owned part of every material (at most
     * all of it, rounded down), then their average, rounded down. Every material counts the same; 100 if nothing is
     * needed, and 100 only when nothing is missing.
     */
    public int completionPercent() {
        if (materials.isEmpty()) {
            return FULL_PERCENT;
        }
        long sum = 0;
        for (MaterialLine line : materials) {
            long owned = Math.min(line.owned(), line.required());
            sum = Math.addExact(sum, Math.multiplyExact(owned, FULL_PERCENT) / line.required());
        }
        return (int) (sum / materials.size());
    }

    /** A needed material; the EXP is one line, in points (D112, point 2). */
    public record MaterialLine(String materialId, String name, MaterialCategory category, long required, long owned,
            long missing) {}

    /** The runs of a source, with the Waveplate and the minutes they take. */
    public record SourceLine(String farmingSourceId, String name, long runs, long waveplates, long minutes) {}
}
