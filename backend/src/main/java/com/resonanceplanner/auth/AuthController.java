package com.resonanceplanner.auth;

import com.resonanceplanner.auth.dto.AuthResponse;
import com.resonanceplanner.auth.dto.LoginRequest;
import com.resonanceplanner.auth.dto.RegisterRequest;
import com.resonanceplanner.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registration and login: the only paths of the personal area that need no token. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione", description = "Registrazione e login. La risposta contiene il token da mandare nell'header Authorization: Bearer.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrazione", description = "Crea un account e restituisce il token valido 24 ore. 409 se email o username sono già usati, 400 se i dati non sono validi.")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prova la demo", description = "Crea un account ospite temporaneo, senza corpo (D134): risponde come la registrazione, con il ruolo GUEST e il token valido 24 ore. L'account non ha una password conosciuta e si cancella da solo dopo 24 ore. 429 RATE_LIMITED se esistono già 50 ospiti.")
    public ApiResponse<AuthResponse> guest() {
        return ApiResponse.success(authService.guest());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Restituisce il token valido 24 ore. 401 BAD_CREDENTIALS con un messaggio unico se email o password non sono corrette.")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
