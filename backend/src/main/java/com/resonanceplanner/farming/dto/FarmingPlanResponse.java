package com.resonanceplanner.farming.dto;

import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.farming.FarmingPlan;
import com.resonanceplanner.farming.FarmingPlan.MaterialLine;
import com.resonanceplanner.farming.FarmingPlan.SourceLine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** The farming calculation of the whole roster (D112, docs/05-api.md). */
public record FarmingPlanResponse(
        List<MaterialNeed> materials,
        @Schema(description = "Le run di ogni fonte, nell'ordine scelto dal calcolo") List<SourceRuns> sources,
        Totals totals,
        @Schema(description = "true se il calcolo usa quantità stimate", example = "true") boolean containsEstimates) {

    public static FarmingPlanResponse from(FarmingPlan plan) {
        return new FarmingPlanResponse(MaterialNeed.listOf(plan), SourceRuns.listOf(plan), Totals.of(plan),
                plan.containsEstimates());
    }

    /** A needed material; the EXP is the line {@code character-exp}, in points. */
    public record MaterialNeed(
            @Schema(description = "Identificativo del materiale; la riga character-exp è l'EXP, in punti",
                    example = "elegy-tacet-core")
            String materialId,
            @Schema(example = "Elegy Tacet Core") String name,
            @Schema(example = "BOSS_DROP") MaterialCategory category,
            @Schema(example = "3") long required,
            @Schema(example = "0") long owned,
            @Schema(example = "3") long missing) {

        static List<MaterialNeed> listOf(FarmingPlan plan) {
            return plan.materials().stream().map(MaterialNeed::from).toList();
        }

        private static MaterialNeed from(MaterialLine line) {
            return new MaterialNeed(line.materialId(), line.name(), line.category(), line.required(), line.owned(),
                    line.missing());
        }
    }

    /** The runs of a source, in the order the calculation chose them. */
    public record SourceRuns(
            @Schema(example = "boss-mourning-aix") String farmingSourceId,
            @Schema(example = "Boss Challenge: Mourning Aix") String name,
            @Schema(example = "2") long runs,
            @Schema(example = "120") long waveplates,
            @Schema(example = "8") long minutes) {

        static List<SourceRuns> listOf(FarmingPlan plan) {
            return plan.sources().stream().map(SourceRuns::from).toList();
        }

        private static SourceRuns from(SourceLine line) {
            return new SourceRuns(line.farmingSourceId(), line.name(), line.runs(), line.waveplates(), line.minutes());
        }
    }

    /** {@code estimatedDays} is null when the user has not chosen a daily play time yet. */
    public record Totals(
            @Schema(example = "120") long waveplates,
            @Schema(example = "8") long minutes,
            @Schema(description = "null se il tempo di gioco non è ancora scelto", nullable = true, example = "1")
            Long estimatedDays) {

        static Totals of(FarmingPlan plan) {
            return new Totals(plan.waveplates(), plan.minutes(), plan.estimatedDays());
        }
    }
}
