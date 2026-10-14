package com.resonanceplanner.farming;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.farming.dto.CharacterRequirementsResponse;
import com.resonanceplanner.farming.dto.FarmingPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The farming calculation of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Farming", description = "Materiali mancanti, run, Waveplate, minuti e giorni stimati per gli obiettivi del roster.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class FarmingController {

    private final FarmingService farmingService;

    public FarmingController(FarmingService farmingService) {
        this.farmingService = farmingService;
    }

    @GetMapping("/farming-plan")
    @Operation(summary = "Il mio piano di farming", description = "Tutto il roster insieme: i materiali che servono (l'EXP è la riga `character-exp`, in punti: le Resonance Potion dell'inventario contano per la loro EXP), le run di ogni fonte nell'ordine del calcolo, le Waveplate, i minuti e i giorni stimati (`null` se il tempo di gioco non è ancora stato scelto). L'inventario si conta una volta sola. `containsEstimates` è vero se una fonte usata ha un drop stimato.")
    public ApiResponse<FarmingPlanResponse> plan(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(farmingService.planFor(user.id()));
    }

    @GetMapping("/roster/{characterId}/requirements")
    @Operation(summary = "Cosa manca a un personaggio", description = "Lo stesso calcolo per un solo personaggio, come se tutto l'inventario fosse per lui, più `completionPercent` (da 0 a 100): la media della parte già posseduta di ogni materiale. 404 se il personaggio non è nel tuo roster.")
    public ApiResponse<CharacterRequirementsResponse> requirements(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String characterId) {
        return ApiResponse.success(farmingService.requirementsFor(user.id(), characterId));
    }
}
