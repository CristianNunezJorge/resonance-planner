package com.resonanceplanner.inventory.dto;

import com.resonanceplanner.inventory.InventoryItem;
import io.swagger.v3.oas.annotations.media.Schema;

/** A material that the user has. Name, category and icon are in the public catalog, which the frontend already has. */
public record InventoryItemResponse(
        @Schema(example = "loongs-pearl") String materialId,
        @Schema(example = "18") int quantity) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.getMaterialId(), item.getQuantity());
    }
}
