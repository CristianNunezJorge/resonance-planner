package com.resonanceplanner.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Error part of an ApiResponse (D46): a fixed code for the frontend, a message in Italian
 * for the user and, only for validation errors, the invalid fields.
 */
public record ApiError(
        @Schema(description = "Codice fisso dell'errore", example = "VALIDATION_ERROR") String code,
        @Schema(description = "Messaggio in italiano per l'utente", example = "Alcuni campi non sono validi.") String message,
        @Schema(description = "Solo per VALIDATION_ERROR: il messaggio di ogni campo sbagliato",
                example = "{\"title\": \"Scrivi un titolo.\"}")
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fields) {}
