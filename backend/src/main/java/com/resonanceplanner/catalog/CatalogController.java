package com.resonanceplanner.catalog;

import com.resonanceplanner.catalog.dto.CharacterDetailResponse;
import com.resonanceplanner.catalog.dto.CharacterSummaryResponse;
import com.resonanceplanner.catalog.dto.FarmingSourceResponse;
import com.resonanceplanner.catalog.dto.GameRulesResponse;
import com.resonanceplanner.catalog.dto.MaterialResponse;
import com.resonanceplanner.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public catalog of the game, readable without login (D49). */
@RestController
@RequestMapping("/api/game")
@Tag(name = "Catalogo", description = "Personaggi, materiali, fonti di farming e regole di gioco. Non serve il login.")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/rules")
    @Operation(summary = "Regole di gioco", description = "Massimi e ricarica di Waveplate e Crystals, livelli di ascensione, opzioni di tempo di gioco.")
    public ApiResponse<GameRulesResponse> getRules() {
        return ApiResponse.success(catalogService.getRules());
    }

    @GetMapping("/characters")
    @Operation(summary = "Elenco dei personaggi", description = "I sei personaggi della demo, in ordine di nome.")
    public ApiResponse<List<CharacterSummaryResponse>> listCharacters() {
        return ApiResponse.success(catalogService.listCharacters());
    }

    @GetMapping("/characters/{id}")
    @Operation(summary = "Dettaglio di un personaggio", description = "Il personaggio con i materiali di ogni ascensione. 404 se l'identificativo non esiste.")
    public ApiResponse<CharacterDetailResponse> getCharacter(@PathVariable String id) {
        return ApiResponse.success(catalogService.getCharacter(id));
    }

    @GetMapping("/materials")
    @Operation(summary = "Elenco dei materiali", description = "Tutti i materiali usati dalla demo.")
    public ApiResponse<List<MaterialResponse>> listMaterials() {
        return ApiResponse.success(catalogService.listMaterials());
    }

    @GetMapping("/farming-sources")
    @Operation(summary = "Fonti di farming", description = "Dove si ottengono i materiali, con il costo in Waveplate e i drop medi; i valori stimati hanno estimated = true.")
    public ApiResponse<List<FarmingSourceResponse>> listFarmingSources() {
        return ApiResponse.success(catalogService.listFarmingSources());
    }
}
