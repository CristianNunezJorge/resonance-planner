package com.resonanceplanner.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envelope of every API response (D46): success flag, data on success, error on failure.
 * Only the pages of a list add the meta part.
 */
public record ApiResponse<T>(
        @Schema(description = "true se la richiesta è riuscita", example = "true") boolean success,
        @Schema(description = "Il risultato; null in caso di errore") T data,
        @Schema(description = "L'errore; null se la richiesta è riuscita", nullable = true,
                types = {"object", "null"}, example = "null")
        ApiError error,
        @Schema(description = "Solo nelle pagine di un elenco (lo storico della chat)", nullable = true,
                types = {"object", "null"}, example = "null")
        @JsonInclude(JsonInclude.Include.NON_NULL) PageMeta meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** One page of a list, with its meta part. */
    public static <T> ApiResponse<T> page(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
