package com.resonanceplanner.event;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.event.dto.AdminEventResponse;
import com.resonanceplanner.event.dto.EventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The management of the events, only for the ADMIN role (D14, D19); Spring Security refuses the others with 403. */
@RestController
@RequestMapping("/api/admin/events")
@Tag(name = "Admin eventi", description = "Creare, modificare e cancellare gli eventi. Solo per il ruolo ADMIN.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    @Operation(summary = "Tutti gli eventi", description = "Gli eventi con la programmazione come è salvata (date fisse o scostamento, durata e ripetizione), non le edizioni calcolate. `code` è l'identificativo degli eventi della demo, `null` per quelli creati dall'admin.")
    public ApiResponse<List<AdminEventResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(adminEventService.list(user.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un evento", description = "Regole di D114: date fisse (al massimo 366 giorni) o ricorrenti, giorni della settimana (nessuno o tutti = ogni giorno), al massimo 10 ricompense e 20 attività, effetto obbligatorio solo per `DOUBLE_DROP`. 400 con tutti i campi sbagliati insieme.")
    public ApiResponse<AdminEventResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody EventRequest request) {
        return ApiResponse.success(adminEventService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un evento", description = "Sostituisce tutto, con le stesse regole della creazione; un evento della demo conserva il suo `code`. 404 se l'evento non esiste.")
    public ApiResponse<AdminEventResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo dell'evento", example = "4") @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return ApiResponse.success(adminEventService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancella un evento", description = "Le attività del calendario collegate restano, senza il collegamento. Un evento della demo cancellato ricompare al riavvio del server (D39). 404 se non esiste.")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Identificativo dell'evento", example = "4") @PathVariable Long id) {
        adminEventService.delete(user.id(), id);
    }
}
