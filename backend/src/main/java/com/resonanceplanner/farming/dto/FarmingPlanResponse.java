package com.resonanceplanner.farming.dto;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingPlan;
import com.resonanceplanner.farming.FarmingPlan.MaterialLine;
import com.resonanceplanner.farming.FarmingPlan.SourceLine;
import java.util.List;

/** The farming calculation of the whole roster (D112, docs/05-api.md). */
public record FarmingPlanResponse(List<MaterialNeed> materials, List<SourceRuns> sources, Totals totals,
        boolean containsEstimates) {

    public static FarmingPlanResponse from(FarmingPlan plan) {
        return new FarmingPlanResponse(MaterialNeed.listOf(plan), SourceRuns.listOf(plan), Totals.of(plan),
                plan.containsEstimates());
    }

    /** A needed material; the EXP is the line {@code character-exp}, in points. */
    public record MaterialNeed(String materialId, String name, MaterialCategory category, long required, long owned,
            long missing) {

        static List<MaterialNeed> listOf(FarmingPlan plan) {
            return plan.materials().stream().map(MaterialNeed::from).toList();
        }

        private static MaterialNeed from(MaterialLine line) {
            return new MaterialNeed(line.materialId(), line.name(), line.category(), line.required(), line.owned(),
                    line.missing());
        }
    }

    /** The runs of a source, in the order the calculation chose them. */
    public record SourceRuns(String farmingSourceId, String name, long runs, long waveplates, long minutes) {

        static List<SourceRuns> listOf(FarmingPlan plan) {
            return plan.sources().stream().map(SourceRuns::from).toList();
        }

        private static SourceRuns from(SourceLine line) {
            return new SourceRuns(line.farmingSourceId(), line.name(), line.runs(), line.waveplates(), line.minutes());
        }
    }

    /** {@code estimatedDays} is null when the user has not chosen a daily play time yet. */
    public record Totals(long waveplates, long minutes, Long estimatedDays) {

        static Totals of(FarmingPlan plan) {
            return new Totals(plan.waveplates(), plan.minutes(), plan.estimatedDays());
        }
    }
}
