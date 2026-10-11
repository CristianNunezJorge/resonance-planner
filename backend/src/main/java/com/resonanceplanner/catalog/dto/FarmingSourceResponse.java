package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.FarmingDrop;
import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.catalog.FarmingSourceType;
import java.math.BigDecimal;
import java.util.List;

/** A farming source with what it drops on average for each run. */
public record FarmingSourceResponse(String id, String name, FarmingSourceType type, Integer waveplateCost,
        Integer minutesPerRun, List<DropResponse> drops) {

    // estimated = true when the quantity is our own estimate (D25): the app shows it as such
    public record DropResponse(String materialId, String materialName, BigDecimal avgQuantity, boolean estimated) {

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
