package com.resonanceplanner.inventory.dto;

import com.resonanceplanner.inventory.InventoryItem;

/** A material that the user has. Name, category and icon are in the public catalog, which the frontend already has. */
public record InventoryItemResponse(String materialId, int quantity) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.getMaterialId(), item.getQuantity());
    }
}
