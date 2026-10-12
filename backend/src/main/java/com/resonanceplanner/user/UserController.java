package com.resonanceplanner.user;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.user.dto.UpdateProfileRequest;
import com.resonanceplanner.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The profile of the logged user: no identifier in the URL, the user is the one of the token (D44). */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Profilo", description = "Dati, modifica ed eliminazione dell'account di chi ha fatto il login.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Il mio profilo", description = "Username, email, ruolo, tempo di gioco e se la configurazione guidata è completata. 401 se il token non è valido o l'account non esiste più.")
    public ApiResponse<UserResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userService.getProfile(user.id()));
    }

    @PatchMapping
    @Operation(summary = "Modifica il profilo", description = "Cambia lo username e/o il tempo di gioco (30, 60 o 120 minuti): i campi che mancano restano come sono, gli altri (email, ruolo...) sono ignorati. 400 se un valore non è valido, 409 se lo username è già usato. Se un valore non è valido non si salva niente.")
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.update(user.id(), request));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina il mio account", description = "Cancella l'account e tutti i suoi dati (roster, inventario, attività, proposte, messaggi). Non si può annullare. 204 senza contenuto.")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user) {
        userService.delete(user.id());
    }
}
