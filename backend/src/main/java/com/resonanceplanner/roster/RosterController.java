package com.resonanceplanner.roster;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.roster.dto.RosterEntryResponse;
import com.resonanceplanner.roster.dto.UpdateCharacterRequest;
import io.swagger.v3.oas.annotations.Operation;
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

/** The roster of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/roster")
@Tag(name = "Roster", description = "I personaggi di chi ha fatto il login, con livello e grado di ascensione attuali e obiettivo.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping
    @Operation(summary = "Il mio roster", description = "I personaggi nell'ordine in cui sono stati aggiunti, con livello e grado attuali e obiettivo. Nome, elemento e icona sono nel catalogo pubblico (`/api/game/characters`).")
    public ApiResponse<List<RosterEntryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(rosterService.list(user.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aggiungi un personaggio", description = "Il personaggio deve essere nel catalogo e non essere già nel tuo roster (409). Livello e grado devono esistere nel gioco (ogni grado ha i suoi livelli, vedi `/api/game/rules`) e l'obiettivo non può essere più basso della situazione attuale: altrimenti 400, con tutti i campi sbagliati.")
    public ApiResponse<RosterEntryResponse> add(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddCharacterRequest request) {
        return ApiResponse.success(rosterService.add(user.id(), request));
    }

    @PutMapping("/{characterId}")
    @Operation(summary = "Modifica livelli e obiettivo", description = "Stesse regole dell'aggiunta. 404 se il personaggio non è nel tuo roster. Se un valore non è valido non si salva niente.")
    public ApiResponse<RosterEntryResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String characterId, @Valid @RequestBody UpdateCharacterRequest request) {
        return ApiResponse.success(rosterService.update(user.id(), characterId, request));
    }

    @DeleteMapping("/{characterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Togli un personaggio", description = "Toglie il personaggio dal roster. 404 se non c'è. 204 senza contenuto.")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String characterId) {
        rosterService.delete(user.id(), characterId);
    }
}
