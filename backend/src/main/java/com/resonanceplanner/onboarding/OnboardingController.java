package com.resonanceplanner.onboarding;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The guided setup of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/onboarding")
@Tag(name = "Configurazione guidata", description = "I tre passi del primo accesso, salvati insieme.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    @Operation(summary = "Completa la configurazione guidata", description = "Salva in un'unica transazione i personaggi con livelli e obiettivi (almeno uno, nessuno ripetuto), le Waveplate lette nel gioco e il tempo di gioco al giorno. Se qualcosa non è valido non si salva niente e il 400 elenca tutti i campi sbagliati (`characters[0].currentLevel`, `waveplate.value`, `playTimeMinutes`...). 409 se la configurazione è già completata. Il roster aggiunto prima dall'API viene sostituito; l'inventario resta com'è. Risponde con l'utente aggiornato.")
    public ApiResponse<UserResponse> complete(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.success(onboardingService.complete(user.id(), request));
    }
}
