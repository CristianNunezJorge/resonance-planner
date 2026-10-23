package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.Material;
import com.resonanceplanner.catalog.MaterialCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/** A material of the catalog; exp is only set for Resonance Potions. */
public record MaterialResponse(
        @Schema(example = "medium-resonance-potion") String id,
        @Schema(example = "Medium Resonance Potion") String name,
        @Schema(example = "CHARACTER_EXP") MaterialCategory category,
        @Schema(description = "Solo per le Resonance Potion: i punti esperienza", nullable = true, example = "3000")
        Integer exp,
        @Schema(description = "null se il materiale non ha un'icona", nullable = true, example = "null") String icon) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(material.getId(), material.getName(), material.getCategory(), material.getExp(),
                material.getIcon());
    }
}
