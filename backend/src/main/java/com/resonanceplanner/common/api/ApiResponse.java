package com.resonanceplanner.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope of every API response (D46): success flag, data on success, error on failure.
 * Only the pages of a list add the meta part.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
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
