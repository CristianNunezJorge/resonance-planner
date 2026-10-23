package com.resonanceplanner.planning;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.planning.dto.ProposalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The proposals of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/proposals")
@Tag(name = "Proposte", description = "Piani del pianificatore in attesa di conferma: si creano con il piano base o con l'agente, poi si confermano o si scartano.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Proposta in attesa", description = "La proposta in attesa con tutte le sue attività, oppure `data` vuoto se non ce n'è.")
    public ApiResponse<ProposalResponse> pending(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(proposalService.pending(user.id()).orElse(null));
    }

    @PostMapping("/basic")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Genera il piano base", description = "Il pianificatore con i parametri predefiniti, senza IA: 7 giorni da oggi con il tuo tempo di gioco. Sostituisce la proposta in attesa. 409 se non c'è niente da pianificare, con l'avviso del pianificatore.")
    public ApiResponse<ProposalResponse> createBasic(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(proposalService.createBasic(user.id()));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Conferma la proposta", description = "Le attività entrano nel calendario e sostituiscono quelle dell'agente confermate da oggi in poi; quelle fatte e quelle manuali restano. 404 se la proposta non è tua; 409 se non è più in attesa o è di un giorno precedente.")
    public ApiResponse<ProposalResponse> confirm(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo della proposta", example = "15") @PathVariable Long id) {
        return ApiResponse.success(proposalService.confirm(user.id(), id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Scarta la proposta", description = "La proposta resta nello storico, le sue attività si cancellano. 404 se la proposta non è tua; 409 se non è più in attesa.")
    public ApiResponse<ProposalResponse> reject(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo della proposta", example = "15") @PathVariable Long id) {
        return ApiResponse.success(proposalService.reject(user.id(), id));
    }
}
