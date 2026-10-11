package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.Material;
import com.resonanceplanner.catalog.MaterialCategory;

/** A material of the catalog; exp is only set for Resonance Potions. */
public record MaterialResponse(String id, String name, MaterialCategory category, Integer exp, String icon) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(material.getId(), material.getName(), material.getCategory(), material.getExp(),
                material.getIcon());
    }
}
