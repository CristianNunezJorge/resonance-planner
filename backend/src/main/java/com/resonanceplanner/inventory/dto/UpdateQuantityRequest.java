package com.resonanceplanner.inventory.dto;

import com.resonanceplanner.inventory.InventoryItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The quantity of a material that the user has: a whole number from 0 up to the maximum that can be saved. */
public record UpdateQuantityRequest(
        @Schema(description = "Da 0 a 999.999.999: sostituisce la quantità salvata", example = "120")
        @NotNull(message = "Inserisci la quantità.")
        @Min(value = 0, message = "La quantità non può essere negativa.")
        @Max(value = InventoryItem.MAX_QUANTITY, message = "La quantità non può superare " + InventoryItem.MAX_QUANTITY + ".")
        Integer quantity) {}
