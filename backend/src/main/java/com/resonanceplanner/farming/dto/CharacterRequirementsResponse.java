package com.resonanceplanner.farming.dto;

import com.resonanceplanner.farming.FarmingPlan;
import com.resonanceplanner.farming.dto.FarmingPlanResponse.MaterialNeed;
import com.resonanceplanner.farming.dto.FarmingPlanResponse.SourceRuns;
import com.resonanceplanner.farming.dto.FarmingPlanResponse.Totals;
import java.util.List;

/**
 * The farming calculation of one character, as if the whole inventory were for it, with the percentage of the need
 * already owned for the progress bar of the roster page (D112, point 7).
 */
public record CharacterRequirementsResponse(String characterId, int completionPercent, List<MaterialNeed> materials,
        List<SourceRuns> sources, Totals totals, boolean containsEstimates) {

    public static CharacterRequirementsResponse from(String characterId, FarmingPlan plan) {
        return new CharacterRequirementsResponse(characterId, plan.completionPercent(), MaterialNeed.listOf(plan),
                SourceRuns.listOf(plan), Totals.of(plan), plan.containsEstimates());
    }
}
