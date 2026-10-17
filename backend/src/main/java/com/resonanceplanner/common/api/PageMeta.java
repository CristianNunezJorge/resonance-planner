package com.resonanceplanner.common.api;

/**
 * The meta part of a page of a list (D46), for example the history of the chat (D129): how many items a page holds at
 * most, and whether older items follow. The next page starts from the last item received, not from a page number.
 */
public record PageMeta(int limit, boolean hasMore) {}
