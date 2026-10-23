package com.resonanceplanner.planning.dto;

import com.resonanceplanner.inventory.dto.InventoryItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A task marked as done and what it added to the inventory (D21, D118): for a FARMING task the quantity of each
 * material it gave, in the order of the drops of the source; empty for the other kinds. Near the highest quantity the
 * inventory stops at 999,999,999, so it can grow by less than the quantity given here.
 */
public record CompletionResponse(
        TaskResponse task,
        @Schema(description = "Quanto ha dato ogni materiale; vuoto per le attività che non sono di farming")
        List<InventoryItemResponse> addedToInventory) {

    public CompletionResponse {
        addedToInventory = List.copyOf(addedToInventory);
    }
}
