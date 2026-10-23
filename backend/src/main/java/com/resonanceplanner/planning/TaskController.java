package com.resonanceplanner.planning;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.planning.dto.CompletionResponse;
import com.resonanceplanner.planning.dto.TaskRequest;
import com.resonanceplanner.planning.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The tasks of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/tasks")
@Tag(name = "Attività", description = "Le attività del calendario: quelle manuali si creano, modificano e cancellano; tutte si possono segnare come fatte.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un'attività manuale", description = "Titolo (fino a 120 caratteri), giorno (dal 2000 al 2100), durata da 1 a 1440 minuti e note facoltative (fino a 1000 caratteri). Nasce già confermata, nel calendario. 400 con tutti i campi sbagliati; 409 se hai già 500 attività manuali.")
    public ApiResponse<TaskResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TaskRequest request) {
        return ApiResponse.success(taskService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un'attività manuale", description = "Stessi campi e regole della creazione; lo stato resta com'è. 404 se l'attività non è tua; 409 se l'ha creata l'agente.")
    public ApiResponse<TaskResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo dell'attività", example = "42") @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ApiResponse.success(taskService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancella un'attività manuale", description = "204 senza contenuto. 404 se l'attività non è tua; 409 se l'ha creata l'agente.")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo dell'attività", example = "42") @PathVariable Long id) {
        taskService.delete(user.id(), id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Segna come fatta", description = "Solo per un'attività confermata del calendario (409 se è già fatta o è ancora una proposta). Un'attività di farming aggiunge all'inventario la quantità media di ogni materiale per le sue run, arrotondata per difetto; se quel giorno un evento potenzia quel tipo di fonte, il moltiplicatore vale per le prime run del giorno fino al suo limite. `addedToInventory` dice cosa è stato aggiunto. Non si può annullare: l'inventario si corregge a mano.")
    public ApiResponse<CompletionResponse> complete(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo dell'attività", example = "42") @PathVariable Long id) {
        return ApiResponse.success(taskService.complete(user.id(), id));
    }
}
