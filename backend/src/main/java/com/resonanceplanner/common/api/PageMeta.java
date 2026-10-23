package com.resonanceplanner.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The meta part of a page of a list (D46), for example the history of the chat (D129): how many items a page holds at
 * most, and whether older items follow. The next page starts from the last item received, not from a page number.
 */
public record PageMeta(
        @Schema(description = "Elementi per pagina al massimo", example = "20") int limit,
        @Schema(description = "true se ci sono elementi più vecchi", example = "true") boolean hasMore) {}
