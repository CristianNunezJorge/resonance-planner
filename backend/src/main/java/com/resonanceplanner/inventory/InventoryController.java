package com.resonanceplanner.inventory;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.inventory.dto.InventoryItemResponse;
import com.resonanceplanner.inventory.dto.UpdateQuantityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The inventory of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/inventory")
@Tag(name = "Inventario", description = "I materiali che chi ha fatto il login possiede.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Il mio inventario", description = "I materiali che hai salvato, con la quantità, in ordine di identificativo. L'inventario parte vuoto: un materiale che non hai mai salvato non c'è nell'elenco (vale 0). Nome, categoria e icona sono nel catalogo pubblico (`/api/game/materials`).")
    public ApiResponse<List<InventoryItemResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(inventoryService.list(user.id()));
    }

    @PutMapping("/{materialId}")
    @Operation(summary = "Correggi la quantità di un materiale", description = "Imposta quante unità hai di quel materiale (un numero intero da 0 a 999999999), al posto della quantità di prima; la prima volta crea la voce. 404 se il materiale non è nel catalogo, 400 se la quantità non è valida.")
    public ApiResponse<InventoryItemResponse> setQuantity(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo del materiale", example = "loongs-pearl")
            @PathVariable String materialId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return ApiResponse.success(inventoryService.setQuantity(user.id(), materialId, request));
    }
}
