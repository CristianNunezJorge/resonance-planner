package com.resonanceplanner.waveplate;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import com.resonanceplanner.waveplate.dto.WaveplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Waveplate counter of the logged user: no identifier in the URL, the user is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/waveplate")
@Tag(name = "Waveplate", description = "Il contatore di Waveplate e Waveplate Crystals di chi ha fatto il login.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class WaveplateController {

    private final WaveplateService waveplateService;

    public WaveplateController(WaveplateService waveplateService) {
        this.waveplateService = waveplateService;
    }

    @GetMapping
    @Operation(summary = "Le mie Waveplate adesso", description = "Valori attuali calcolati dall'ultima lettura: le Waveplate si rigenerano fino al massimo e solo allora i Crystals ricominciano ad accumulare. `fullAt` e `crystalsFullAt` sono i momenti in cui i due contatori sono (o sono stati) pieni: se il contatore è già pieno sono nel passato. 404 se non hai ancora salvato nessuna lettura.")
    public ApiResponse<WaveplateResponse> get(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(waveplateService.get(user.id()));
    }

    @PutMapping
    @Operation(summary = "Salva i valori letti nel gioco", description = "Waveplate e Crystals letti nel gioco (se i Crystals mancano valgono 0). Il momento della lettura lo sceglie il server. 400 se un valore non è possibile nel gioco o non è un numero intero.")
    public ApiResponse<WaveplateResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateWaveplateRequest request) {
        return ApiResponse.success(waveplateService.update(user.id(), request));
    }
}
