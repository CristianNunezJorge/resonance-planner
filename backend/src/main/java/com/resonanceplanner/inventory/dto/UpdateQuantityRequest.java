package com.resonanceplanner.inventory.dto;

import com.resonanceplanner.inventory.InventoryItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The quantity of a material that the user has: a whole number from 0 up to the maximum that can be saved. */
public record UpdateQuantityRequest(
        @NotNull(message = "Inserisci la quantità.")
        @Min(value = 0, message = "La quantità non può essere negativa.")
        @Max(value = InventoryItem.MAX_QUANTITY, message = "La quantità non può superare " + InventoryItem.MAX_QUANTITY + ".")
        Integer quantity) {}
