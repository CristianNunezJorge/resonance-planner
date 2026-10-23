package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.FarmingDrop;
import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.catalog.FarmingSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** A farming source with what it drops on average for each run. */
public record FarmingSourceResponse(
        @Schema(example = "boss-mourning-aix") String id,
        @Schema(example = "Boss Challenge: Mourning Aix") String name,
        @Schema(example = "BOSS_CHALLENGE") FarmingSourceType type,
        @Schema(description = "Waveplate per run; 0 per nemici e raccolta", example = "60") Integer waveplateCost,
        @Schema(example = "4") Integer minutesPerRun,
        List<DropResponse> drops) {

    // estimated = true when the quantity is our own estimate (D25): the app shows it as such
    public record DropResponse(
            @Schema(example = "advanced-resonance-potion") String materialId,
            @Schema(example = "Advanced Resonance Potion") String materialName,
            @Schema(description = "Quantità media per run", example = "2.18") BigDecimal avgQuantity,
            @Schema(description = "true se la quantità è una stima", example = "false") boolean estimated) {

        static DropResponse from(FarmingDrop drop) {
            return new DropResponse(drop.getMaterial().getId(), drop.getMaterial().getName(), drop.getAvgQuantity(),
                    drop.isEstimated());
        }
    }

    public static FarmingSourceResponse from(FarmingSource source) {
        return new FarmingSourceResponse(source.getId(), source.getName(), source.getType(), source.getWaveplateCost(),
                source.getMinutesPerRun(), source.getDrops().stream().map(DropResponse::from).toList());
    }
}
