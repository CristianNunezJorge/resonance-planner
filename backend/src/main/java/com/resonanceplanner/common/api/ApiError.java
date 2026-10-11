package com.resonanceplanner.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Error part of an ApiResponse (D46): a fixed code for the frontend, a message in Italian
 * for the user and, only for validation errors, the invalid fields.
 */
public record ApiError(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fields) {}
