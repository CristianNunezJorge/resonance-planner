package com.resonanceplanner.common.error;

import com.resonanceplanner.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exists only in the tests: one endpoint for each kind of error the handler must translate. */
@RestController
@RequestMapping("/test")
class ErrorTestController {

    record ItemRequest(
            @NotBlank(message = "Il nome è obbligatorio.") String name,
            @Min(value = 1, message = "La quantità deve essere almeno 1.") int quantity) {}

    @GetMapping("/ok")
    ApiResponse<Map<String, String>> ok() {
        return ApiResponse.success(Map.of("value", "ok"));
    }

    @PostMapping("/items")
    ApiResponse<ItemRequest> create(@Valid @RequestBody ItemRequest request) {
        return ApiResponse.success(request);
    }

    @GetMapping("/items/{id}")
    ApiResponse<Void> find(@PathVariable long id) {
        throw new NotFoundException("Oggetto non trovato.");
    }

    @GetMapping("/conflict")
    ApiResponse<Void> conflict() {
        throw new ConflictException("Oggetto già presente.");
    }

    @GetMapping("/range")
    ApiResponse<Integer> range(@RequestParam @Min(value = 1, message = "Indica almeno un giorno.") @Max(62) int days) {
        return ApiResponse.success(days);
    }

    @GetMapping("/crash")
    ApiResponse<Void> crash() {
        throw new IllegalStateException("internal detail that must stay in the logs");
    }

    // Body and path variable validated together: Spring raises HandlerMethodValidationException
    @PostMapping("/items/{id}")
    ApiResponse<ItemRequest> update(
            @PathVariable @Min(value = 1, message = "L'identificativo deve essere positivo.") long id,
            @Valid @RequestBody ItemRequest request) {
        return ApiResponse.success(request);
    }

    // What a @Validated controller or service throws
    @GetMapping("/constraint-violation")
    ApiResponse<Void> constraintViolation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        throw new ConstraintViolationException(validator.validate(new ItemRequest("", 0)));
    }

    @GetMapping("/bad-credentials")
    ApiResponse<Void> badCredentials() {
        throw new BadCredentialsException("Bad credentials");
    }

    @GetMapping("/access-denied")
    ApiResponse<Void> accessDenied() {
        throw new AccessDeniedException("Access denied");
    }

    @GetMapping("/too-many-requests")
    ApiResponse<Void> tooManyRequests() {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }
}
